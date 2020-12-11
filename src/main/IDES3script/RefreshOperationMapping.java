package IDES3script;

import ides.api.plugin.operation.OperationManager;

import javax.script.ScriptEngine;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.stream.Collectors;

public class RefreshOperationMapping {
  private RefreshOperationMapping() {}

  @FunctionalInterface
  public interface VarArgFunction<R, T> {
    @SuppressWarnings("unused") // this method is called from nashorn
    R apply(T... args);
  }

  //public static String refreshOperationMapping(Value jsBindings) {
  public static String refreshOperationMapping(ScriptEngine engine) {
    return OperationManager.instance().getOperationNames().stream().map(op -> {
      String legalId = convert(op);

      if (OperationManager.instance().getOperation(op).getNumberOfOutputs() == 1)
        engine.put(legalId,
          (VarArgFunction<Object, Object>) args -> ExecuteScript.operation1(op, args));
      else
        engine.put(legalId,
          (VarArgFunction<Object, Object>) args -> ExecuteScript.operation(op, args));

      if (op.compareTo(legalId) != 0)
        return op + " --> " + legalId;
      else
        return op;
    }).collect(Collectors.joining("\n"));
  }

  // https://stackoverflow.com/a/7441035
  private static String convert(String ident) {
    if (ident.length() == 0) {
      return "_";
    }
    CharacterIterator ci = new StringCharacterIterator(ident);
    StringBuilder sb = new StringBuilder();
    for (char c = ci.first(); c != CharacterIterator.DONE; c = ci.next()) {
      if (c == ' ')
        c = '_';
      if (sb.length() == 0) {
        if (Character.isJavaIdentifierStart(c)) {
          sb.append(c);
          continue;
        } else
          sb.append('_');
      }
      if (Character.isJavaIdentifierPart(c)) {
        sb.append(c);
      } else {
        sb.append('_');
      }
    }
    return sb.toString();
  }
}
