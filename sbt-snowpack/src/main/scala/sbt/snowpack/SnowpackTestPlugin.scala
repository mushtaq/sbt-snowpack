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
    lazy val snowpackTestServer         = settingKey[SnowpackTestServer]("process handle of the test server")
    lazy val startSnowpackTestServer    = taskKey[Unit]("start snowpack test server")
    lazy val stopSnowpackTestServer     = taskKey[Unit]("stop snowpack test server")
    lazy val reStartSnowpackTestServer  = taskKey[Unit]("restart snowpack test server")
    lazy val generateSnowpackTestConfig = taskKey[Path]("generate snowpack test config")
  }

  import autoImport._

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    snowpackTestServer := new SnowpackTestServer(baseDirectory.value, crossTarget.value, name.value),
    startSnowpackTestServer := {
      val _ = (Test / fastOptJS).value
      snowpackTestServer.value.start()
    },
    stopSnowpackTestServer := snowpackTestServer.value.stop(),
    reStartSnowpackTestServer := {
      val _ = stopSnowpackTestServer.value
      startSnowpackTestServer.value
    },
    generateSnowpackTestConfig := {
      snowpackTestServer.value.generateTestConfig()
      snowpackTestServer.value.testConfigPath
    },
    jsEnv := snowpackTestServer.value.seleniumJsEnv,
    fastOptJS / crossTarget := snowpackTestServer.value.snowpackMountDir.toFile,
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule).withSourceMap(false) },
    Global / onLoad := {
      (Global / onLoad).value.compose {
        _.addExitHook(snowpackTestServer.value.stop())
      }
    }
  )
}
