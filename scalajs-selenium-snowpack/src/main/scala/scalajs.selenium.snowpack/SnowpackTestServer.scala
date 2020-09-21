package scalajs.selenium.snowpack

import java.io.File
import java.lang.ProcessBuilder.Redirect
import java.nio.file.Files

import org.scalajs.jsenv.selenium.SeleniumJSEnv

class SnowpackTestServer(baseDir: File, crossTarget: File, testPort: Int, extraArgs: List[String], enableStdout: Boolean) {
  private val contentDirName = "test-run"
  private val contentDir     = s"$crossTarget/$contentDirName"
  private val webRoot        = s"http://localhost:$testPort/"

  @volatile
  private var process: Option[Process] = None
  sys.addShutdownHook(process.foreach(_.destroy()))

  private val snowpackTestConfig: String =
    s"""
       |{
       |  "mount": {
       |    "$contentDir" : "/",
       |    "$crossTarget" : "/_dist_"
       |  },
       |  "devOptions": {
       |    "port": $testPort,
       |    "open": "none",
       |    "hmr": false
       |  }
       |}
       |""".stripMargin

  def start(): Unit =
    synchronized {
      Files.createDirectories(crossTarget.toPath)
      val testConfigPath = crossTarget.toPath.resolve("snowpack.test.config.json")
      Files.write(testConfigPath, snowpackTestConfig.getBytes())

      val commands           = List("npm", "start", "--", "--config", testConfigPath.toString) ++ extraArgs
      val baseProcessBuilder = new ProcessBuilder(commands: _*).directory(baseDir)

      val processBuilder =
        if (enableStdout) baseProcessBuilder.inheritIO()
        else baseProcessBuilder.redirectError(Redirect.INHERIT)

      val command = commands.mkString(" ")

      process match {
        case Some(value) =>
          println(s"snowpack test server already running on port:$testPort and pid:${value.pid()}")
          println(s"command used: '$command'")
        case None        =>
          println(s"starting snowpack: '$command'")
          process = Some(processBuilder.start())
      }
    }

  def stop(): Unit =
    synchronized {
      process match {
        case Some(value) =>
          println("stopping snowpack test server")
          value.destroy()
        case None        =>
          println(s"snowpack test server is already stopped")
      }
      process = None
    }

  def seleniumConfig: SeleniumJSEnv.Config = {
    SeleniumJSEnv
      .Config()
      .withMaterializeInServer(contentDir, webRoot)
  }
}
