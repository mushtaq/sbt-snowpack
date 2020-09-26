import Libs._

import scala.collection.JavaConverters.asScalaBufferConverter

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
    scriptedLaunchOpts ++= {
      val list = java.lang.management.ManagementFactory.getRuntimeMXBean.getInputArguments.asScala.toList
      list.filter(a => Seq("-Xmx", "-Xms", "-XX", "-Dsbt.log.noformat").exists(a.startsWith))
    },
    scriptedBufferLog := false,
    sbtPlugin := true,
    publishMavenStyle := true,
    publishM2 := {
      val _                = publishM2.value
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
    ),
    libraryDependencies += "com.github.mushtaq.scala-js-env-selenium" %% "scalajs-env-selenium" % "5374c6b",
    libraryDependencies += "com.typesafe.play"                        %% "play-json"            % "2.9.1",
    addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.2.0")
  )

lazy val `example` = project
  .enablePlugins(ScalaJSPlugin, SnowpackPlugin)
  .settings(
    libraryDependencies ++= Seq(
      scalatest.value % Test,
      `scala-async`,
      ScalablyTyped.R.rxjs
    ),
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
    Test / test := {
      reStartSnowpackServer.dependsOn((Test / fastOptJS)).value
      (Test / test).value
      (Test / testHtml).value
    },
    Compile / run := {
      reStartSnowpackServer.dependsOn((Compile / fastOptJS)).value
      (Compile / run).toTask("").value
    }
  )
