package IDES3script

object Log {
  def logDebug(obj: Any): Unit = {
    log("DEBUG", obj)
  }

  def logError(obj: Any): Unit = {
    log("ERROR", obj)
  }

  def log(`type`: String, obj: Any): Unit = {
    val pos = new Throwable()
      .getStackTrace
      .dropWhile((frame: StackTraceElement) =>
        frame.getClass == this.getClass)
      .headOption match {
        case Some(frame) =>
          val file = frame.getFileName
          val lineN = frame.getLineNumber
          s"$file:$lineN"
        case None => s"unknown"
      }

    val msg = obj.toString
    System.out.println(s"[ ${`type`} ] at $pos $msg")

    //        val postFunc =
    //        if (`type`.toUpperCase == "ERROR")
    //          Hub.getNoticeManager.postErrorUntilRevoked
    //        else
    //          Hub.getNoticeManager.postInfoUntilRevoked

    //postFunc.apply("[" + type + "] at " + file + ":" + lineN, msg);
  }
}