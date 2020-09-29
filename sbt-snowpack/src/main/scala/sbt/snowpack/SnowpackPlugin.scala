package sbt.snowpack

import java.nio.file.Path

import org.openqa.selenium.chrome.ChromeOptions
import org.scalajs.jsenv.selenium.SeleniumJSEnv
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt.Keys._
import sbt._

object SnowpackPlugin extends AutoPlugin {
  override val trigger: PluginTrigger = noTrigger

  override val requires: Plugins = plugins.JvmPlugin && ScalaJSPlugin

  object autoImport {
    lazy val snowpackServer                 = settingKey[SnowpackServer]("process handle of the server")
    lazy val startSnowpackServer            = taskKey[Unit]("start snowpack server")
    lazy val stopSnowpackServer             = taskKey[Unit]("stop snowpack server")
    lazy val reStartSnowpackServer          = taskKey[Unit]("restart snowpack server")
    lazy val generateInternalSnowpackConfig = taskKey[Path]("generate snowpack config for startSnowpackServer task")
    lazy val generateExternalSnowpackConfig = taskKey[Path]("generate snowpack config for npm start command")
  }

  import autoImport._

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule).withSourceMap(false) },
    snowpackServer := new SnowpackServer(
      new SnowpackInternalConfig(
        baseDirectory.value,
        crossTarget.value,
        name.value
      )
    ),
    stopSnowpackServer := snowpackServer.value.stop(),
    generateExternalSnowpackConfig := {
      val a = (Compile / fastOptJS).value
      new SnowpackExternalConfig(
        baseDirectory.value,
        crossTarget.value,
        name.value,
        (LocalRootProject / baseDirectory).value
      ).generateTestConfig()
    },
    jsEnv := {
      new SeleniumJSEnv(
        new ChromeOptions().setHeadless(true),
        snowpackServer.value.seleniumConfig
      )
    },
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
      configuration / generateInternalSnowpackConfig := {
        val a = (configuration / fastOptJS).value
        snowpackServer.value.snowpackConfig.generateTestConfig()
      },
      configuration / startSnowpackServer := {
        val a = (configuration / generateInternalSnowpackConfig).value
        snowpackServer.value.start()
      },
      configuration / reStartSnowpackServer := {
        val _ = stopSnowpackServer.value
        (configuration / startSnowpackServer).value
      }
    )
}
