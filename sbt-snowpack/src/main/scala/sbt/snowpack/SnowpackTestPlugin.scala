package sbt.snowpack

import sbt._
import sbt.Keys._

object SnowpackTestPlugin extends AutoPlugin {
  override val trigger: PluginTrigger = noTrigger

  override val requires: Plugins = plugins.JvmPlugin

  object autoImport {
    lazy val testPort                = settingKey[Int]("port number to be used by snowpack test server")
    lazy val extraArgs               = settingKey[List[String]]("extra arguments for snowpack test server")
    lazy val enableStdout            = settingKey[Boolean]("show snowpack test server stdout stream")
    lazy val snowpackTestServer      = settingKey[SnowpackTestServer]("process handle of the test server")
    lazy val startSnowpackTestServer = taskKey[Unit]("start snowpack test server")
    lazy val stopSnowpackTestServer  = taskKey[Unit]("stop snowpack test server")
  }

  import autoImport._

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    testPort := 9091,
    extraArgs := Nil,
    enableStdout := false,
    snowpackTestServer := new SnowpackTestServer(
      baseDirectory.value,
      crossTarget.value,
      testPort.value,
      extraArgs.value,
      enableStdout.value
    ),
    startSnowpackTestServer := snowpackTestServer.value.start(),
    stopSnowpackTestServer := snowpackTestServer.value.stop()
  )
}
