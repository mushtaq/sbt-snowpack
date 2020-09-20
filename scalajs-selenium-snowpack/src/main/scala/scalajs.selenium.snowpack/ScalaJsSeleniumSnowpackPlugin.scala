package scalajs.selenium.snowpack

import sbt._
import sbt.Keys._

object ScalaJsSeleniumSnowpackPlugin extends AutoPlugin {
  override val trigger: PluginTrigger = noTrigger

  override val requires: Plugins = plugins.JvmPlugin

  object autoImport {
    lazy val snowpackTestServer      = settingKey[SnowpackTestServer]("process handle of the test server")
    lazy val startSnowpackTestServer = taskKey[Unit]("start snowpack test server")
    lazy val stopSnowpackTestServer  = taskKey[Unit]("stop snowpack test server")
  }

  import autoImport._

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    snowpackTestServer := new SnowpackTestServer(baseDirectory.value),
    startSnowpackTestServer := snowpackTestServer.value.start(),
    stopSnowpackTestServer := snowpackTestServer.value.stop()
  )
}
