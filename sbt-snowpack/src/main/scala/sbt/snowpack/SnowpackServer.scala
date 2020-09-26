package sbt.snowpack

import java.io.File
import java.lang.ProcessBuilder.Redirect
import java.nio.file.{Files, Path}

import org.openqa.selenium.chrome.ChromeOptions
import org.scalajs.jsenv.selenium.SeleniumJSEnv
import play.api.libs.json.{JsArray, JsObject, Json}

abstract class SnowpackServer(projectBaseDir: File, crossTarget: File, configName: String) {
  protected def pluginJson: List[JsArray]
  protected def baseJson: JsObject

  @volatile
  private var process: Option[Process] = None
  sys.addShutdownHook(process.foreach(_.destroy()))

  private val configFileName   = s"snowpack.${configName}.config.json"
  val snowpackMountDir: Path   = crossTarget.toPath.resolve("snowpack").resolve(configName)
  private val configPath: Path = snowpackMountDir.resolve(configFileName)

  private val startCommand    = List("npx", "snowpack", "dev", "--config", configPath.toString, "--reload")
  private val startCommandStr = startCommand.mkString(" ")
  private val userConfigPath  = projectBaseDir.toPath.resolve(configFileName)

  private def readUserConfig(): JsObject = {
    if (userConfigPath.toFile.exists()) {
      Json.parse(Files.readString(userConfigPath)).as[JsObject]
    } else Json.obj()
  }

  private def configJson(): JsObject = {
    val userJson    = readUserConfig()
    val userPlugins = (userJson \ "plugins").asOpt[List[JsArray]].getOrElse(Nil)
    val allPlugins  = Json.obj("plugins" -> (pluginJson ::: userPlugins))
    baseJson.deepMerge(userJson) ++ allPlugins
  }

  private def readPort(): Int = (configJson() \ "devOptions" \ "port").asOpt[Int].getOrElse(8080)

  def generateTestConfig(): Path =
    synchronized {
      Files.createDirectories(snowpackMountDir)
      Files.write(configPath, Json.prettyPrint(configJson()).getBytes())
      println(s"generated config: $configPath")
      println(s"usage: '$startCommandStr'")
      configPath
    }

  def start(): Unit =
    synchronized {
      generateTestConfig()
      val port           = readPort()
      val processBuilder = new ProcessBuilder(startCommand: _*)
        .directory(projectBaseDir)
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
