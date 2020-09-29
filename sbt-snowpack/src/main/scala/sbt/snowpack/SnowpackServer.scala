package sbt.snowpack

import java.lang.ProcessBuilder.Redirect
import java.nio.file.Files

import org.scalajs.jsenv.selenium.SeleniumJSEnv

class SnowpackServer(val snowpackConfig: SnowpackConfig) {
  @volatile
  private var process: Option[Process] = None
  sys.addShutdownHook(process.foreach(_.destroy()))
  private val logFile                  = snowpackConfig.crossTarget.toPath.resolve("snowpack.log")

  private def readPort(): Int = {
    val lookupResult = snowpackConfig.configJson() \ "devOptions" \ "port"
    lookupResult.asOpt[Int].getOrElse(8080)
  }

  def start(): Unit =
    synchronized {
      val processBuilder = {
        val builder = new ProcessBuilder(snowpackConfig.startCommand: _*).directory(snowpackConfig.projectBaseDir)
        Files.deleteIfExists(logFile)
        Files.createFile(logFile)
        builder.redirectError(Redirect.INHERIT).redirectOutput(logFile.toFile)
      }

      val port = readPort()

      process match {
        case Some(value) =>
          println(s"snowpack server already running on port:$port and pid:${value.pid()}")
        case None        =>
          println(s"starting snowpack server on port: $port using above command")
          process = Some(processBuilder.start())
      }
    }

  def stop(): Unit =
    synchronized {
      process match {
        case Some(value) =>
          println(s"stopping snowpack server")
          value.destroy()
        case None        =>
          println(s"snowpack server is already stopped")
      }
      process = None
    }

  def seleniumConfig: SeleniumJSEnv.Config = {
    val contentDirName = "selenium"
    val webRoot        = s"http://localhost:${readPort()}/$contentDirName/"
    val contentDir     = s"${snowpackConfig.crossTarget}/$contentDirName"
    SeleniumJSEnv
      .Config()
      .withMaterializeInServer(contentDir, webRoot)
  }
}
