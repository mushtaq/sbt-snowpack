package sbt.snowpack

import java.io.File
import java.lang.ProcessBuilder.Redirect
import java.nio.file.{Files, Path}

import org.openqa.selenium.chrome.ChromeOptions
import org.scalajs.jsenv.selenium.SeleniumJSEnv

import scala.util.control.NonFatal

class SnowpackTestServer(baseDir: File, crossTarget: File, projectName: String) {
  @volatile
  private var process: Option[Process] = None
  sys.addShutdownHook(process.foreach(_.destroy()))

  val snowpackMountDir: Path = crossTarget.toPath.resolve("snowpack")
  val testConfigPath: Path   = snowpackMountDir.resolve("snowpack.test.config.json")

  private val startCommand    = List("npx", "snowpack", "dev", "--config", testConfigPath.toString, "--reload")
  private val startCommandStr = startCommand.mkString(" ")
  private val userConfigPath  = baseDir.toPath.resolve("snowpack.test.config.json")

  def testPort(): Int = {
    try {
      val json = ujson.read(Files.readString(userConfigPath))
      json("devOptions")("port").num.toInt
    } catch {
      case NonFatal(_) => 9091
    }
  }

  private def snowpackTestConfig(port: Int) = {
    val extendsClause = ujson.Obj(
      "extends" -> userConfigPath.toString
    )

    val baseJson = ujson.Obj(
      "mount"      -> ujson.Obj(
        snowpackMountDir.toString                      -> "/",
        s"$crossTarget/$projectName-fastopt-test-html" -> "/testHtml"
      ),
      "devOptions" -> ujson.Obj(
        "port" -> port,
        "open" -> "none",
        "hmr"  -> false
      )
    )

    val json = if (userConfigPath.toFile.exists()) ujson.Obj(extendsClause.obj ++= baseJson.obj) else baseJson
    ujson.write(json)
  }

  def generateTestConfig(): Int =
    synchronized {
      val port = testPort()
      Files.createDirectories(snowpackMountDir)
      Files.write(testConfigPath, snowpackTestConfig(port).getBytes())
      println(s"generated config: $testConfigPath")
      println(s"usage: '$startCommandStr'")
      port
    }

  def start(): Unit =
    synchronized {
      val port           = generateTestConfig()
      val processBuilder = new ProcessBuilder(startCommand: _*)
        .directory(baseDir)
        .redirectError(Redirect.INHERIT)

      process match {
        case Some(value) =>
          println(s"snowpack test server already running on port:$port and pid:${value.pid()}")
        case None        =>
          println(s"starting snowpack test server on port: $port using above command")
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
    val contentDirName = "test-run"
    val webRoot        = s"http://localhost:${testPort()}/$contentDirName/"
    val contentDir     = s"$snowpackMountDir/$contentDirName"

    new SeleniumJSEnv(
      new ChromeOptions().setHeadless(true),
      SeleniumJSEnv
        .Config()
        .withMaterializeInServer(contentDir, webRoot)
    )
  }
}
