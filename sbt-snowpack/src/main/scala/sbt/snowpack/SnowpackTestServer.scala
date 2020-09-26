package sbt.snowpack

import java.io.File

import play.api.libs.json.{JsArray, JsObject, Json}

class SnowpackTestServer(projectBaseDir: File, crossTarget: File, projectName: String)
    extends SnowpackServer(projectBaseDir, crossTarget, "test") {

  override protected def pluginJson: List[JsArray] = Nil

  protected def baseJson: JsObject =
    Json.obj(
      "mount"      -> Json.obj(
        snowpackMountDir.toString                      -> "/",
        s"$crossTarget/$projectName-fastopt-test-html" -> "/testHtml"
      ),
      "devOptions" -> Json.obj(
        "open" -> "none",
        "hmr"  -> false
      )
    )
}
