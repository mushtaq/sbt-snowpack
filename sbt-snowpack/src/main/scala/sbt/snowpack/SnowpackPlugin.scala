package sbt.snowpack

import java.nio.file.Path

import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt.Keys._
import sbt._

object SnowpackPlugin extends AutoPlugin {
  override val trigger: PluginTrigger = noTrigger

  override val requires: Plugins = plugins.JvmPlugin && ScalaJSPlugin

  object autoImport {
    lazy val snowpackServer         = settingKey[SnowpackServer]("process handle of the test server")
    lazy val startSnowpackServer    = taskKey[Unit]("start snowpack test server")
    lazy val stopSnowpackServer     = taskKey[Unit]("stop snowpack test server")
    lazy val reStartSnowpackServer  = taskKey[Unit]("restart snowpack test server")
    lazy val generateSnowpackConfig = taskKey[Path]("generate snowpack test config")
  }

  import autoImport._

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule).withSourceMap(false) },
    Compile / snowpackServer := new SnowpackDevServer(
      baseDirectory.value,
      crossTarget.value,
      name.value,
      (LocalRootProject / baseDirectory).value
    ),
    Test / snowpackServer := new SnowpackTestServer(
      baseDirectory.value,
      crossTarget.value,
      name.value
    )
  ) ++ inConfig(Compile)(commonSettings(Compile)) ++ inConfig(Test)(commonSettings(Test))

  private def commonSettings(config: Configuration): Seq[Setting[_]] = {
    val configSnowpackServer = config / snowpackServer
    Seq(
      startSnowpackServer := {
        val _ = (config / fastOptJS).value
        configSnowpackServer.value.start()
      },
      stopSnowpackServer := configSnowpackServer.value.stop(),
      reStartSnowpackServer := {
        val _ = stopSnowpackServer.value
        startSnowpackServer.value
      },
      generateSnowpackConfig := configSnowpackServer.value.generateTestConfig(),
      jsEnv := configSnowpackServer.value.seleniumJsEnv,
      fastOptJS / crossTarget := configSnowpackServer.value.snowpackMountDir.toFile,
      Global / onLoad := {
        (Global / onLoad).value.compose {
          _.addExitHook {
            configSnowpackServer.value.stop()
          }
        }
      }
    )
  }
}
