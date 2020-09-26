package sbt.snowpack

import java.io.File
import java.lang.ProcessBuilder.Redirect
import java.nio.file.{Files, Path}

import org.openqa.selenium.chrome.ChromeOptions
import org.scalajs.jsenv.selenium.SeleniumJSEnv
import ujson.Obj

import scala.util.control.NonFatal

class SnowpackTestServer(baseDir: File, crossTarget: File, projectName: String) extends SnowpackServer(baseDir, crossTarget, "test") {
  protected def baseJson(port: Int): Obj =
    ujson.Obj(
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
}

abstract class SnowpackServer(baseDir: File, crossTarget: File, configName: String) {
  protected def baseJson(port: Int): Obj

  @volatile
  private var process: Option[Process] = None
  sys.addShutdownHook(process.foreach(_.destroy()))

  private val configFileName = s"snowpack.${configName}.config.json"
  val snowpackMountDir: Path = crossTarget.toPath.resolve("snowpack").resolve(configName)
  val configPath: Path       = snowpackMountDir.resolve(configFileName)

  private val startCommand    = List("npx", "snowpack", "dev", "--config", configPath.toString, "--reload")
  private val startCommandStr = startCommand.mkString(" ")
  private val userConfigPath  = baseDir.toPath.resolve(configFileName)

  def readPort(): Int = {
    try {
      val json = ujson.read(Files.readString(userConfigPath))
      json("devOptions")("port").num.toInt
    } catch {
      case NonFatal(_) => 8080
    }
  }

  private def snowpackConfigJson(port: Int) = {
    val extendsClause = ujson.Obj("extends" -> userConfigPath.toString)
    val json = {
      if (userConfigPath.toFile.exists()) ujson.Obj(extendsClause.obj ++= baseJson(port).obj)
      else baseJson(port)
    }
    ujson.write(json)
  }

  def generateTestConfig(): Int =
    synchronized {
      val port = readPort()
      Files.createDirectories(snowpackMountDir)
      Files.write(configPath, snowpackConfigJson(port).getBytes())
      println(s"generated config: $configPath")
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
          println(s"snowpack $configName server already running on port:$port and pid:${value.pid()}")
        case None        =>
          println(s"starting snowpack $configName server on port: $port using above command")
          process = Some(processBuilder.start())
      }
    }

  def stop(): Unit =
    synchronized {
      process match {
        case Some(value) =>
          println(s"stopping snowpack $configName server")
          value.destroy()
        case None        =>
          println(s"snowpack $configName server is already stopped")
      }
      process = None
    }

  def seleniumJsEnv: SeleniumJSEnv = {
    val contentDirName = "selenium"
    val webRoot        = s"http://localhost:${readPort()}/$contentDirName/"
    val contentDir     = s"$snowpackMountDir/$contentDirName"

    new SeleniumJSEnv(
      new ChromeOptions().setHeadless(true),
      SeleniumJSEnv
        .Config()
        .withMaterializeInServer(contentDir, webRoot)
    )
  }
}
