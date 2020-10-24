import Libs._
import sbt.io.Path.userHome
import sbt.librarymanagement.Patterns

import scala.collection.JavaConverters.asScalaBufferConverter

lazy val LocalMavenResolverForSbtPlugins = {
  // remove scala and sbt versions from the path, as it does not work with jitpack
  val pattern  = "[organisation]/[module]/[revision]/[module]-[revision](-[classifier]).[ext]"
  val name     = "local-maven-for-sbt-plugins"
  val location = userHome / ".m2" / "repository"
  Resolver.file(name, location)(Patterns().withArtifactPatterns(Vector(pattern)))
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
    resolvers += LocalMavenResolverForSbtPlugins,
    publishM2Configuration := publishM2Configuration.value.withResolverName(LocalMavenResolverForSbtPlugins.name),
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
    // below settings are duplicated in both build.sbt and plugins.sbt
    // this is due to recursive source dependency in plugins.sbt via 'unmanagedSourceDirectories'
    // recursion is *very* convenient as it allows a test example and plugin itself to exist in the same repo
    libraryDependencies += "com.typesafe.play" %% "play-json"            % "2.9.1",
    libraryDependencies += "org.scala-js"      %% "scalajs-env-selenium" % "1.1.0",
    // note, 'sbt-scalajs' must come after 'scalajs-env-selenium'
    // reference: https://github.com/scala-js/scala-js-env-selenium#usage
    addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.3.0")
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
      (Test / reStartSnowpackServer).value
      (Test / test).value
      (Test / testHtml).value
      ()
    },
    Compile / run := {
      (Compile / reStartSnowpackServer).value
      (Compile / run).toTask("").value
    }
  )
