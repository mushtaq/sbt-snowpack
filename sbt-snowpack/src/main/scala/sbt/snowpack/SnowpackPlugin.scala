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
    lazy val snowpackServer            = settingKey[SnowpackServer]("process handle of the server")
    lazy val startSnowpackServer       = taskKey[Unit]("start snowpack server")
    lazy val stopSnowpackServer        = taskKey[Unit]("stop snowpack server")
    lazy val reStartSnowpackServer     = taskKey[Unit]("restart snowpack server")
    lazy val generateSnowpackConfig    = taskKey[Path]("generate snowpack test config")
    lazy val generateSnowpackDevConfig = taskKey[Path]("generate snowpack dev config")
  }

  import autoImport._

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule).withSourceMap(false) },
    snowpackServer := new SnowpackServer(
      new SnowpackTestConfig(
        baseDirectory.value,
        crossTarget.value,
        name.value
      )
    ),
    stopSnowpackServer := snowpackServer.value.stop(),
    generateSnowpackDevConfig := {
      (Compile / fastOptJS).value
      new SnowpackDevConfig(
        baseDirectory.value,
        crossTarget.value,
        name.value,
        (LocalRootProject / baseDirectory).value
      ).generateTestConfig()
    },
    jsEnv := snowpackServer.value.seleniumJsEnv,
    Global / onLoad := {
      (Global / onLoad).value.compose {
        _.addExitHook {
          snowpackServer.value.stop()
        }
      }
    }
  ) ++ configSpecificSettings(Compile) ++ configSpecificSettings(Test)

  def configSpecificSettings(configuration: Configuration) =
    Seq(
      configuration / generateSnowpackConfig := {
        (configuration / fastOptJS).value
        snowpackServer.value.snowpackConfig.generateTestConfig()
      },
      configuration / startSnowpackServer := {
        (configuration / generateSnowpackConfig).value
        snowpackServer.value.start()
      },
      configuration / reStartSnowpackServer := {
        val _ = stopSnowpackServer.value
        (configuration / startSnowpackServer).value
      }
    )
}
