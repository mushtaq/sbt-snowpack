package sbt.snowpack

import java.nio.file.Path

import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt.Keys._
import sbt._

object SnowpackTestPlugin extends AutoPlugin {
  override val trigger: PluginTrigger = noTrigger

  override val requires: Plugins = plugins.JvmPlugin && ScalaJSPlugin

  object autoImport {
    lazy val testPort                   = settingKey[Int]("port number to be used by snowpack test server")
    lazy val snowpackTestServer         = settingKey[SnowpackTestServer]("process handle of the test server")
    lazy val startSnowpackTestServer    = taskKey[Unit]("start snowpack test server")
    lazy val stopSnowpackTestServer     = taskKey[Unit]("stop snowpack test server")
    lazy val reStartSnowpackTestServer  = taskKey[Unit]("restart snowpack test server")
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
    stopSnowpackTestServer := snowpackTestServer.value.stop(),
    reStartSnowpackTestServer := snowpackTestServer.value.restart(),
    generateSnowpackTestConfig := snowpackTestServer.value.generateTestConfig(),
    Global / onLoad := {
      (Global / onLoad).value.compose {
        _.addExitHook(snowpackTestServer.value.stop())
      }
    },
    jsEnv := snowpackTestServer.value.seleniumJsEnv,
    fastOptJS / crossTarget := snowpackTestServer.value.snowpackMountDir.toFile,
    startSnowpackTestServer := {
      val _ = (Test / fastOptJS).value
      snowpackTestServer.value.start()
    },
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule).withSourceMap(false) }
  )
}
