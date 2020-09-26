package sbt.snowpack

import java.io.File

import play.api.libs.json.{JsArray, JsObject, Json}

class SnowpackDevServer(projectBaseDir: File, crossTarget: File, projectName: String, rootBaseDir: File)
    extends SnowpackServer(projectBaseDir, crossTarget, "dev") {

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
        "public"                  -> "/",
        snowpackMountDir.toString -> "/_dist_"
      )
    )
  }
}
