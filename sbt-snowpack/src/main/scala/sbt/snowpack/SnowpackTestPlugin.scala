package sbt.snowpack

import java.nio.file.Path

import sbt.Keys._
import sbt._

object SnowpackTestPlugin extends AutoPlugin {
  override val trigger: PluginTrigger = noTrigger

  override val requires: Plugins = plugins.JvmPlugin

  object autoImport {
    lazy val testPort = settingKey[Int]("port number to be used by snowpack test server")
    lazy val snowpackTestServer = settingKey[SnowpackTestServer]("process handle of the test server")
    lazy val startSnowpackTestServer = taskKey[Unit]("start snowpack test server")
    lazy val stopSnowpackTestServer = taskKey[Unit]("stop snowpack test server")
    lazy val generateSnowpackTestConfig = taskKey[Path]("generate snowpack test config")
  }

  import autoImport._

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    testPort := 9091,
    snowpackTestServer := new SnowpackTestServer(
      baseDirectory.value,
      crossTarget.value,
      testPort.value
    ),
    startSnowpackTestServer := snowpackTestServer.value.start(),
    stopSnowpackTestServer := snowpackTestServer.value.stop(),
    generateSnowpackTestConfig := snowpackTestServer.value.generateTestConfig()
  )
}
