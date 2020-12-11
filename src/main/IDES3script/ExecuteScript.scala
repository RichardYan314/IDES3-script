package IDES3script

import java.io.{ByteArrayOutputStream, File, PrintStream}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.util
import java.util.Objects
import java.util.function.Predicate
import java.util.prefs.Preferences

import ExecuteScript._
import TryWithPeek._
import com.esotericsoftware.yamlbeans.YamlReader
import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine
import ides.api.core.Hub
import ides.api.model.fsa.FSAModel
import ides.api.plugin.layout.FSALayoutManager
import ides.api.plugin.model.{DESEventSet, DESModel, ModelManager}
import ides.api.plugin.operation.{Operation, OperationManager}
import ides.api.presentation.fsa.FSAStateLabeller
import javax.script.ScriptEngine
import javax.swing.JFileChooser
import org.graalvm.polyglot.{Context, HostAccess, Value}

import scala.annotation.varargs
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}
import cats.implicits._

class ExecuteScript extends Operation {
  private val LAST_USED_FOLDER = "LAST_USED_FOLDER"

  override def getName: String = "execute script"
  override def getDescription: String = "Execute script."

  override def getNumberOfInputs: Int = 0
  override def getTypeOfInputs: Array[Class[_]] = Array()
  override def getDescriptionOfInputs: Array[String] = Array()

  override def getNumberOfOutputs: Int = 0
  override def getTypeOfOutputs: Array[Class[_]] = Array()
  override def getDescriptionOfOutputs: Array[String] = Array()

  override def getWarnings: util.List[String] = new util.LinkedList()

  override def perform(objects: Array[AnyRef]): Array[AnyRef] = {
    // In case this plugin is loaded before some operation plugins
    refreshOperationMapping(engine)

    // select script file
    Try({
      val prefs = Preferences.userRoot.node(getClass.getName)
      val fc = new JFileChooser(prefs.get(LAST_USED_FOLDER, new File(".").getAbsolutePath))
      if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
        prefs.put(LAST_USED_FOLDER, fc.getSelectedFile.getParent)
      else
        throw new Exception("Open command cancelled by user.")
      fc.getSelectedFile
    })
    .peek(Log.logDebug)
    // load script file
    .map(_.toPath)
    .peek((p: Path) => currentScriptPath = p)
    .map(Files.readString)
    // prepend predefined identifiers
    .map((s: String) =>
      // doesn't nashorn support val/let?
      s"""
        |var IDES = IDES_class.static;
        |var Hub = Hub_class.static;
        |var OperationManager = OperationManager_class.static;
        |var FSAStateLabeller = FSAStateLabeller_class.static;
        |
        |$s
        |""".stripMargin
    )
    // evaluate script
    .map(s =>
      Foo.runInPluginContext(() => {
        engine.eval(s)
    })) match {
      case Failure(e) =>
        // TODO: handle exceptions
        val os = new ByteArrayOutputStream
        val ps = new PrintStream(os)
        e.printStackTrace(ps)
        val stackTrace = os.toString(StandardCharsets.UTF_8)
        Hub.displayAlert(e.getMessage)
        Log.logError(e.getMessage + "\n" + stackTrace)
      case _ =>
    }

    Array() // return nothing
  }

  var engine: ScriptEngine = _

  def init(): Unit = {
    Foo.runInPluginContext(() => {
      val builder = Context
        .newBuilder("js")
        .allowHostAccess(HostAccess
          .newBuilder(HostAccess.ALL)
          // Goal: [] -> Object
          // When the target type is "exactly" <java.lang.Object> and the source is a array like object (json array for
          // example) then we want it to be a <java.util.List>, otherwise the default behavior of graal is <java.util.Map>
          .targetTypeMapping(
            classOf[java.util.List[Any]]
              : Class[util.List[Any]], // TODO is there a better class for here?
            classOf[Object]
              : Class[Object],
            (v => Objects.nonNull(v))
              : Predicate[util.List[Any]],
            (v => v.asScala.toList)
              : util.function.Function[util.List[Any], Object],
            HostAccess.TargetMappingPrecedence.HIGHEST
          )
          // Goal: [...] -> List
          // Convert array like object to <scala.List>
          .targetTypeMapping(
            classOf[Value]
              : Class[Value],
            classOf[List[Any]]
              : Class[List[Any]],
            (_.hasArrayElements)
              : Predicate[Value],
            (_.as(classOf[List[Any]]))
              : util.function.Function[Value, List[Any]],
            HostAccess.TargetMappingPrecedence.HIGH
          )
          .build()
        )

      engine = GraalJSScriptEngine.create(null, builder)
      engine.put("greeting", "Hi")
      engine.eval("console.log(greeting + ' world')")
    })

    engine.put("greeting", "Hello")
    Foo.runInPluginContext(() => {
      engine.eval("console.log(greeting + ' world')")
    })

    engine.put("IDES_class", classOf[ExecuteScript])
    engine.put("Hub_class", classOf[Hub])
    engine.put("OperationManager_class", classOf[OperationManager])
    engine.put("FSAStateLabeller_class", classOf[FSAStateLabeller])
    engine.put("DESModel_class", classOf[DESModel])
  }
}

object ExecuteScript {
  private var currentScriptPath: Path = _

  // cannot get scala lambda functions to work with varargs
  // so use java instead
  def refreshOperationMapping(engine: ScriptEngine): String =
    RefreshOperationMapping.refreshOperationMapping(engine)

  //  def refreshOperationMapping(jsBindings: Value): String =
  //    RefreshOperationMapping.refreshOperationMapping(jsBindings)
  //OperationManager
  //  .instance
  //  .getOperationNames
  //  .stream
  //  .map((op: String) => {
  //    val legalId = convert(op)
  //    if (OperationManager
  //      .instance
  //      .getOperation(op)
  //      .getNumberOfOutputs == 1
  //    ) {
  //      @varargs def o(args: AnyRef*) = {
  //        operation1(op, null, null, args:_*)
  //      }
  //
  //      //engine.put(legalId, o(_));
  //      engine.put(
  //        legalId,
  //        (args: Seq[AnyRef]) =>
  //          operation1(op, null, null, args:_*)
  //        .asInstanceOf[JExecuteScript.VarArgFunction[AnyRef, AnyRef]]
  //      )
  //    } else {
  //      @varargs def o(args: AnyRef*) = {
  //        operation(op, null, null, args:_*)
  //      }
  //
  //      engine.put(legalId, o(_));
  //      //engine.put(
  //      //  legalId,
  //      //  (args: Seq[AnyRef]) =>
  //      //    operation(op, null, null, args:_*)
  //      //      .asInstanceOf[JExecuteScript.VarArgFunction[AnyRef, AnyRef]]
  //      //)
  //    }
  //
  //    if (op.compareTo(legalId) != 0)
  //      op + " --> " + legalId
  //    else
  //      op
  //  })
  //  .peek(println(_))
  //  .collect(Collectors.joining("\n"))

  def removeAllNoPrompt(): Unit = {
    // close all models without asking for save
    Hub.getWorkspace.getModels
      .forEachRemaining(model => {
        // trick the model to think it is saved
        // to avoid prompt of unsaved model
        model.modelSaved()
        removeModel(model)
      })
  }

  @HostAccess.Export
  def importYAML(s: String): FSAModel = {
    System.out.println("importYAML[String] called with " + s)
    Try(currentScriptPath.getParent)
      .map(cwd =>
        cwd.resolve(s))
      .map(Files.readString)
      .map(declare)
      .get
  }

  def parseSpec(spec: String): Try[FSASpec] =
    Try(new YamlReader(spec))
      .map(_.read)
      .peek(println)
      .map {
        case null => new util.HashMap()
        case v => v
      }
      .map(FSASpec.validate)
      .flatMap(e =>
        e.fold(
          errors =>
            // TODO: handle exceptions
            Failure(new IllegalArgumentException(
              errors.toIterable.toList.mkString("\n")
            )),
          fsaSpec =>
            Success(fsaSpec)
        )
      )

  def declare(spec: String): FSAModel =
    parseSpec(spec)
      .map(spec => declare(spec))
      .get

  def declare(spec: FSASpec): FSAModel =
    Try(spec)
      .map(
        spec => spec.toFSAModel()
      ) match {
        case Failure(e) =>
          // TODO: handle exceptions
          val os = new ByteArrayOutputStream
          val ps = new PrintStream(os)
          e.printStackTrace(ps)
          val stackTrace = os.toString(StandardCharsets.UTF_8)
          Log.logError(e.getMessage + "\n" + stackTrace)
          throw e
        case Success(fsa) =>
          addModel(fsa, spec.layouter)
          fsa
      }

  @varargs def operation1(opname: String, args: Any*): Any = {
    val outputs = operation(opname, args: _*)
    if (outputs.length == 1) outputs(0)
    else throw new IllegalStateException(s"Operation $opname yielded ${outputs.length} outputs. Call `operation` if you except an array.")
  }

  @varargs def operation(opname: String, args: Any*): Array[AnyRef] = {
    val op = OperationManager.instance.getOperation(opname)
    val inputTypes = op.getTypeOfInputs
    val inputs = new util.ArrayList[AnyRef]
    for (i <- 0 until inputTypes.length) {
      val clazz = inputTypes(i)
      if (i == inputTypes.length - 1 && op.getNumberOfInputs < 0) { // last unbounded argument
        args.drop(i).foreach((obj: Any) => {
          if (clazz.isAssignableFrom(classOf[FSAModel])) {
            if (classOf[String].isAssignableFrom(obj.getClass))
              inputs.add(Hub.getWorkspace.getModel(obj.asInstanceOf[String]))
            else if (classOf[FSAModel].isAssignableFrom(obj.getClass))
              inputs.add(obj.asInstanceOf[FSAModel])
            else { // TODO
              Log.logError("unsupported input type:" + obj.getClass)
              throw new UnsupportedOperationException
            }
          } else if (clazz.isAssignableFrom(classOf[DESEventSet]) && obj.isInstanceOf[List[String]]) {
            val events = obj
              .asInstanceOf[List[String]]
              .map(classOf[String].cast)
            val eventSet = ModelManager.instance.createEmptyEventSet
            val fsa = ModelManager.instance.createModel(classOf[FSAModel])
            events.foreach((e: String) => eventSet.add(fsa.assembleEvent(e)))
            inputs.add(eventSet)
          } else {
            Log.logError("unsupported input type")
            throw new UnsupportedOperationException
          }
        })
      } else {
        val obj = args(i)
        if (clazz.isAssignableFrom(classOf[FSAModel])) {
          if (classOf[String].isAssignableFrom(obj.getClass))
            inputs.add(Hub.getWorkspace.getModel(obj.asInstanceOf[String]))
          else if (classOf[FSAModel].isAssignableFrom(obj.getClass)) inputs.add(obj.asInstanceOf[FSAModel])
          else {
            Log.logError("unsupported input type")
            throw new UnsupportedOperationException
          }
        } else if (clazz.isAssignableFrom(classOf[DESEventSet]) && obj.isInstanceOf[List[String]]) {
          val events = args(i)
            .asInstanceOf[List[String]]
            .map(classOf[String].cast)
          val eventSet = ModelManager.instance.createEmptyEventSet
          val fsa = ModelManager.instance.createModel(classOf[FSAModel])
          events.foreach((e: String) => eventSet.add(fsa.assembleEvent(e)))
          inputs.add(eventSet)
        } else {
          Log.logError("unsupported input type")
          throw new UnsupportedOperationException
        }
      }
    }

    val name =
        s"$opname (" + {
          inputs
            .asScala
            .filter(_.isInstanceOf[DESModel])
            .map(_.asInstanceOf[DESModel])
            .map(_.getName)
            .mkString(",")
        } +
        ")"

    val outputs = op.perform(inputs.toArray)
    outputs
      .zipWithIndex
      .foreach({ case (output: Any, idx: Int) =>
        if (!classOf[DESModel].isAssignableFrom(output.getClass))
          throw new IllegalStateException(s"Output $idx is not a DESModel.")
        else {
          val outModel = output.asInstanceOf[DESModel]
          outModel.setName(name)
          // D:/My Document/Programs/IDES3script/IDES3/IDES3.jar!/ui/OperationDialog.class:189
          outModel match {
            case model: FSAModel => FSAStateLabeller.labelCompositeStates(model)
          }
          addModel(outModel)
        }
      })

    outputs
  }

  /**
   * Convert string to identifier
   *
   * Unused.
   *
   * @deprecated RefreshOperationMapping#convert(String)
   *
   * @param ident
   * @return
   */
  @deprecated
  private def convert(ident: String): String = {
    ident
      .replaceFirst("$[^a-zA-Z_]+", "_")
      .replaceAll("\\W+", "_")
  }

  def setDefaultLayouter(layouterName: String): Unit = {
    if (layouterName != null) {
      val layouter = FSALayoutManager.instance.getLayouter(layouterName)
      if (layouter != null) FSALayoutManager.instance.setDefaultLayouter(layouter)
      else Log.logError("Layouter " + layouterName + " does not exists.")
    }
  }

  def addModel(model: DESModel): Unit = addModel(model, null)

  def addModel(model: DESModel, layouterName: String): Unit = {
    setDefaultLayouter(layouterName)
    Hub.getWorkspace.addModel(model)
    Hub.getWorkspace.setActiveModel(model.getName)
  }

  def removeModel(model: DESModel): Unit = {
    Hub.getWorkspace.removeModel(model.getName)
  }
}

class TryWithPeek[T] (t : Try[T]) {
  def peek(f : T => Any): Try[T] = t match {
    case Success(v) =>
      f(v)
      t
    case _ => t
  }
}

object TryWithPeek {
  implicit def toTryWithPeek[T](t: Try[T]): TryWithPeek[T] =
    new TryWithPeek(t)
}

object Foo {
  def runInPluginContext(callback: () => Any): Unit = {
    val oldCl: ClassLoader = Thread.currentThread.getContextClassLoader
    Thread.currentThread.setContextClassLoader(IDES3script.loader)
    callback()
    Thread.currentThread.setContextClassLoader(oldCl)
  }
}