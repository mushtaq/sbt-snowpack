package sbt.snowpack

import java.io.File
import java.nio.file.{Files, Path}

import play.api.libs.json.{JsArray, JsObject, Json}

trait SnowpackConfig {
  def projectBaseDir: File
  def crossTarget: File
  def configName: String

  protected def pluginJson: List[JsArray]
  protected def baseJson: JsObject

  private val configFileName   = s"snowpack.${configName}.config.json"
  private val configPath: Path = crossTarget.toPath.resolve(configFileName)

  val startCommand            = List("npx", "snowpack", "dev", "--config", configPath.toString, "--reload")
  private val startCommandStr = startCommand.mkString(" ")
  private val userConfigPath  = projectBaseDir.toPath.resolve(configFileName)

  private def readUserConfig(): JsObject = {
    if (userConfigPath.toFile.exists()) {
      Json.parse(Files.readString(userConfigPath)).as[JsObject]
    } else Json.obj()
  }

  def configJson(): JsObject = {
    val userJson       = readUserConfig()
    val userExtends    = (userJson \ "extends").asOpt[String]
    val extendsJson    = userExtends
      .map(x => projectBaseDir.toPath.resolve(Path.of(x)))
      .map(x => Json.parse(Files.readString(x)).as[JsObject])
      .getOrElse(Json.obj())
    val mergedUserJson = extendsJson.deepMerge(userJson) - "extends"

    val userPlugins = (mergedUserJson \ "plugins").asOpt[List[JsArray]].getOrElse(Nil)
    val allPlugins  = Json.obj("plugins" -> (pluginJson ::: userPlugins))
    baseJson.deepMerge(mergedUserJson) ++ allPlugins
  }

  def generateTestConfig(): Path =
    synchronized {
      Files.createDirectories(crossTarget.toPath)
      Files.write(configPath, Json.prettyPrint(configJson()).getBytes())
      println(s"generated $configName config file, run using command:")
      println(startCommandStr)
      configPath
    }
}

class SnowpackInternalConfig(
    val projectBaseDir: File,
    val crossTarget: File,
    val projectName: String
) extends SnowpackConfig {
  override def configName: String = "internal"

  override protected def pluginJson: List[JsArray] = Nil

  protected def baseJson: JsObject =
    Json.obj(
      "mount"      -> Json.obj(
        crossTarget.toString                           -> "/",
        s"$crossTarget/$projectName-fastopt-test-html" -> "/testHtml"
      ),
      "devOptions" -> Json.obj(
        "open" -> "none",
        "hmr"  -> false
      )
    )
}

class SnowpackExternalConfig(
    val projectBaseDir: File,
    val crossTarget: File,
    val projectName: String,
    val rootBaseDir: File
) extends SnowpackConfig {

  override def configName: String = "external"

  protected def pluginJson: List[JsArray] =
    List(
      Json.arr(
        "@snowpack/plugin-run-script",
        Json.obj(
          "cmd"   -> s"set -x; cd $rootBaseDir; sbtn $projectName/fastOptJS; cd -",
          "watch" -> s"set -x; cd $rootBaseDir; sbtn ~$projectName/fastOptJS; cd -"
        )
      )
    )

  protected def baseJson: JsObject = {
    Json.obj(
      "mount" -> Json.obj(
        projectBaseDir.toPath.resolve("public").toString -> "/",
        crossTarget.toString                             -> "/dist"
      )
    )
  }
}
