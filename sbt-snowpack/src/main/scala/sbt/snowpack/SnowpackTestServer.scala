package sbt.snowpack

import java.io.File
import java.lang.ProcessBuilder.Redirect
import java.nio.file.{Files, Path}

import org.openqa.selenium.chrome.ChromeOptions
import org.scalajs.jsenv.selenium.SeleniumJSEnv
import play.api.libs.json.{JsObject, Json}

class SnowpackTestServer(projectBaseDir: File, crossTarget: File, projectName: String)
    extends SnowpackServer(projectBaseDir, crossTarget, "test") {
  protected def baseJson(port: Int): JsObject =
    Json.obj(
      "mount"      -> Json.obj(
        snowpackMountDir.toString                      -> "/",
        s"$crossTarget/$projectName-fastopt-test-html" -> "/testHtml"
      ),
      "devOptions" -> Json.obj(
        "port" -> port,
        "open" -> "none",
        "hmr"  -> false
      )
    )
}

class SnowpackDevServer(projectBaseDir: File, crossTarget: File, projectName: String, rootBaseDir: File)
    extends SnowpackServer(projectBaseDir, crossTarget, "dev") {
  protected def baseJson(port: Int): JsObject =
    Json.obj(
      "mount"      -> Json.obj(
        "public"                  -> "/",
        snowpackMountDir.toString -> "/_dist_"
      ),
      "devOptions" -> Json.obj(
        "port" -> port
      ),
      "plugins"    -> Json.arr(
        Json.arr(
          "@snowpack/plugin-run-script",
          Json.obj(
            "cmd"   -> s"set -x; cd $rootBaseDir; sbtn $projectName/fastOptJS; cd -",
            "watch" -> s"set -x; cd $rootBaseDir; sbtn ~$projectName/fastOptJS; cd -"
          )
        )
      )
    )
}

abstract class SnowpackServer(projectBaseDir: File, crossTarget: File, configName: String) {
  protected def baseJson(port: Int): JsObject

  @volatile
  private var process: Option[Process] = None
  sys.addShutdownHook(process.foreach(_.destroy()))

  private val configFileName = s"snowpack.${configName}.config.json"
  val snowpackMountDir: Path = crossTarget.toPath.resolve("snowpack").resolve(configName)
  val configPath: Path       = snowpackMountDir.resolve(configFileName)

  private val startCommand    = List("npx", "snowpack", "dev", "--config", configPath.toString, "--reload")
  private val startCommandStr = startCommand.mkString(" ")
  private val userConfigPath  = projectBaseDir.toPath.resolve(configFileName)

  def readPort(): Int = {
    if (userConfigPath.toFile.exists()) {
      val json = Json.parse(Files.readString(userConfigPath))
      (json \ "devOptions" \ "port").asOpt[Int].getOrElse(8080)
    } else 8080
  }

  private def snowpackConfigJson(port: Int) = {
    val extendsClause = Json.obj("extends" -> userConfigPath.toString)
    val json = {
      if (userConfigPath.toFile.exists()) extendsClause ++ baseJson(port)
      else baseJson(port)
    }
    Json.prettyPrint(json)
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
