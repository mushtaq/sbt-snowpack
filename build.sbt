import Libs._
import org.openqa.selenium.chrome.ChromeOptions
import org.scalajs.jsenv.selenium.SeleniumJSEnv

inThisBuild(
  Seq(
    version := "0.1.0-SNAPSHOT",
    organization := "com.github.mushtaq.sbt-snowpack",
    organizationName := "ThoughtWorks",
    resolvers += "jitpack" at "https://jitpack.io",
    scalafmtOnCompile := true,
    licenses := Seq(
      ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))
    )
  )
)

lazy val `sbt-snowpack-root` = project
  .in(file("."))
  .aggregate(`sbt-snowpack`, `example`)

lazy val `sbt-snowpack` = project
  .enablePlugins(ScriptedPlugin)
  .settings(
    scalaVersion := "2.12.12",
    scriptedLaunchOpts += ("-Dplugin.version=" + version.value),
    scriptedLaunchOpts ++= sys.process.javaVmArguments.filter(a => Seq("-Xmx", "-Xms", "-XX", "-Dsbt.log.noformat").exists(a.startsWith)),
    scriptedBufferLog := false,
    sbtPlugin := true,
    publishMavenStyle := true,
    publishM2 := {
      publishM2.value
      val orgPath          = organization.value.replace(".", "/")
      val basePath         = s".m2/repository/$orgPath/${name.value}"
      val originalLocation = file(sys.env("HOME")) / s"${basePath}_${scalaBinaryVersion.value}_${sbtBinaryVersion.value}"
      val newLocation      = file(sys.env("HOME")) / s"$basePath"
      println(s"originalLocation ----------> $originalLocation")
      println(s"newLocation      ----------> $newLocation")
      originalLocation.renameTo(newLocation)
    },
    scalacOptions ++= Seq(
      "-encoding",
      "UTF-8",
      "-feature",
      "-unchecked",
      "-deprecation",
      "-Xlint:-unused,_",
      "-Ywarn-dead-code",
      "-Xfuture"
    )
  )

lazy val `example` = project
  .enablePlugins(ScalaJSPlugin, SnowpackTestPlugin)
  .settings(
    libraryDependencies ++= Seq(
      scalatest.value % Test,
      `scala-async`,
      ScalablyTyped.R.rxjs
    ),
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule).withSourceMap(false) },
    scalaVersion := "2.13.3",
    scalacOptions ++= Seq(
      "-encoding",
      "UTF-8",
      "-feature",
      "-unchecked",
      "-deprecation",
      "-Wconf:any:warning-verbose",
      "-Wdead-code",
      "-Xlint:_,-missing-interpolator",
      "-Xsource:3",
      "-Xcheckinit"
      //      "-Xasync" does not work with Scala.js js yet
    ),
    crossTarget in fastOptJS := snowpackTestServer.value.snowpackMountDir.toFile,
    startSnowpackTestServer := startSnowpackTestServer.dependsOn(Test / fastOptJS).value,
    jsEnv := new SeleniumJSEnv(
      new ChromeOptions().setHeadless(true),
      SeleniumJSEnv
        .Config()
        .withMaterializeInServer(snowpackTestServer.value.contentDir, snowpackTestServer.value.webRoot)
    )
  )
