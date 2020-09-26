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
  private val configPath: Path = crossTarget.toPath.resolve(configFileName)

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
    val userExtends = (userJson \ "extends").asOpt[String]
    val extendsJson = userExtends
      .map(x => projectBaseDir.toPath.resolve(Path.of(x)))
      .map(x => Json.obj("extends" -> x.toString))
      .getOrElse(Json.obj())

    val allPlugins = Json.obj("plugins" -> (pluginJson ::: userPlugins))
    baseJson.deepMerge(userJson) ++ extendsJson ++ allPlugins
  }

  private def readPort(): Int = (configJson() \ "devOptions" \ "port").asOpt[Int].getOrElse(8080)

  def generateTestConfig(): Path =
    synchronized {
      Files.createDirectories(crossTarget.toPath)
      Files.write(configPath, Json.prettyPrint(configJson()).getBytes())
      println(s"generated $configName config file, run using command:")
      println(startCommandStr)
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
    val contentDir     = s"$crossTarget/$contentDirName"

    new SeleniumJSEnv(
      new ChromeOptions().setHeadless(true),
      SeleniumJSEnv
        .Config()
        .withMaterializeInServer(contentDir, webRoot)
    )
  }
}
