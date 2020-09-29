import Libs._
import sbt.io.Path.userHome
import sbt.librarymanagement.Patterns

import scala.collection.JavaConverters.asScalaBufferConverter

lazy val localMavenResolverForSbtPlugins = {
  val mavenPatternForSbtPlugins = "[organisation]/[module]/[revision]/[module]-[revision](-[classifier]).[ext]"
  Resolver.file("local-maven-for-sbt-plugins", userHome / ".m2" / "repository")(
    Patterns().withArtifactPatterns(Vector(mavenPatternForSbtPlugins))
  )
}

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
  .enablePlugins(SbtPlugin)
  .settings(
    scalaVersion := "2.12.12",
    scriptedLaunchOpts += ("-Dplugin.version=" + version.value),
    scriptedLaunchOpts ++= {
      val list = java.lang.management.ManagementFactory.getRuntimeMXBean.getInputArguments.asScala.toList
      list.filter(a => Seq("-Xmx", "-Xms", "-XX", "-Dsbt.log.noformat").exists(a.startsWith))
    },
    scriptedBufferLog := false,
    publishMavenStyle := true,
    resolvers += localMavenResolverForSbtPlugins,
    publishM2Configuration := publishM2Configuration.value.withResolverName(localMavenResolverForSbtPlugins.name),
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
    libraryDependencies += "com.typesafe.play" %% "play-json"            % "2.9.1",
    libraryDependencies += "org.scala-js"      %% "scalajs-env-selenium" % "1.1.0",
    // note, 'sbt-scalajs' must come after 'scalajs-env-selenium'
    // reference: https://github.com/scala-js/scala-js-env-selenium#usage
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
      val ()  = (Test / reStartSnowpackServer).value
      val ()  = (Test / test).value
      val (_) = (Test / testHtml).value
    },
    Compile / run := {
      val () = (Compile / reStartSnowpackServer).value
      val () = (Compile / run).toTask("").value
    }
  )
