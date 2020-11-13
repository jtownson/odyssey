import sbt.{Logger, file}

import scala.sys.process.Process

object exec {
  def runExec(exec: String, command: String, workingDir: String, logger: Logger, extraEnv: (String, String)*): Unit = {
    val execCommand = s"$exec $command"
    logger.info(s"Running '$execCommand' in $workingDir")
    logger.debug(extraEnv.mkString(", "))
    val rc = Process(execCommand, file(workingDir), extraEnv: _*).!
    if (rc != 0) {
      val errorMsg = s"$execCommand returned non-zero return code: $rc"
      logger.error(errorMsg)
      sys.error(errorMsg)
    }
  }
}
