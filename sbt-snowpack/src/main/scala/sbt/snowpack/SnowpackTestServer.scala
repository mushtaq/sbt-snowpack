package sbt.snowpack

import java.io.File
import java.lang.ProcessBuilder.Redirect
import java.nio.file.{Files, Path}
import org.openqa.selenium.chrome.ChromeOptions
import org.scalajs.jsenv.selenium.SeleniumJSEnv

class SnowpackTestServer(baseDir: File, crossTarget: File, testPort: Int) {
  private val contentDirName = "test-run"
  val webRoot                = s"http://localhost:$testPort/$contentDirName/"
  val snowpackMountDir: Path = crossTarget.toPath.resolve("snowpack")
  val contentDir             = s"$snowpackMountDir/$contentDirName"

  private val testConfigPath  = snowpackMountDir.resolve("snowpack.test.config.json")
  private val userConfigPath  = baseDir.toPath.resolve("snowpack.test.config.json")
  private val startCommand    = List("npx", "snowpack", "dev", "--config", testConfigPath.toString, "--reload")
  private val startCommandStr = startCommand.mkString(" ")

  @volatile
  private var process: Option[Process] = None
  sys.addShutdownHook(process.foreach(_.destroy()))

  private def extendsClause = if (userConfigPath.toFile.exists()) s""""extends": "$userConfigPath",""" else ""

  private def snowpackTestConfig: String =
    s"""{
       |  $extendsClause
       |  "mount": {
       |    "$snowpackMountDir" : "/"
       |  },
       |  "devOptions": {
       |    "port": $testPort,
       |    "open": "none",
       |    "hmr": false
       |  }
       |}
       |""".stripMargin

  def generateTestConfig(): Path =
    synchronized {
      Files.createDirectories(snowpackMountDir)
      Files.write(testConfigPath, snowpackTestConfig.getBytes())
      println(s"generated config: $testConfigPath")
      println(s"usage: '$startCommandStr'")
      testConfigPath
    }

  def start(): Unit =
    synchronized {
      generateTestConfig()
      val processBuilder = new ProcessBuilder(startCommand: _*)
        .directory(baseDir)
        .redirectError(Redirect.INHERIT)

      process match {
        case Some(value) =>
          println(s"snowpack test server already running on port:$testPort and pid:${value.pid()}")
        case None        =>
          println(s"starting snowpack using above command")
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

  def seleniumJsEnv: SeleniumJSEnv = {
    new SeleniumJSEnv(
      new ChromeOptions().setHeadless(true),
      SeleniumJSEnv
        .Config()
        .withMaterializeInServer(contentDir, webRoot)
    )
  }
}
