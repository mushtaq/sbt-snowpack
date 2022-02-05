import Libs._
import org.openqa.selenium.chrome.ChromeOptions
import org.scalajs.jsenv.selenium.SeleniumJSEnv
import sbt.io.Path.userHome
import sbt.librarymanagement.Patterns

import _root_.io.github.bonigarcia.wdm.WebDriverManager
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
    version           := "0.1.0-SNAPSHOT",
    organization      := "com.github.mushtaq.sbt-snowpack",
    organizationName  := "ThoughtWorks",
    resolvers += "jitpack" at "https://jitpack.io",
    scalafmtOnCompile := true,
    licenses          := Seq(
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
    scalaVersion                               := "2.12.13",
    scriptedLaunchOpts += ("-Dplugin.version=" + version.value),
    scriptedLaunchOpts ++= {
      val list = java.lang.management.ManagementFactory.getRuntimeMXBean.getInputArguments.asScala.toList
      list.filter(a => Seq("-Xmx", "-Xms", "-XX", "-Dsbt.log.noformat").exists(a.startsWith))
    },
    scriptedBufferLog                          := false,
    publishMavenStyle                          := true,
    resolvers += LocalMavenResolverForSbtPlugins,
    publishM2Configuration                     := publishM2Configuration.value.withResolverName(LocalMavenResolverForSbtPlugins.name),
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
    libraryDependencies += "com.typesafe.play" %% "play-json"            % "2.9.2",
    libraryDependencies += "org.scala-js"      %% "scalajs-env-selenium" % "1.1.1",
    // note, 'sbt-scalajs' must come after 'scalajs-env-selenium'
    // reference: https://github.com/scala-js/scala-js-env-selenium#usage
    addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.7.1")
  )

lazy val `example` = project
  .enablePlugins(ScalaJSPlugin, SnowpackPlugin)
  .settings(
    libraryDependencies ++= Seq(
      scalatest.value % Test,
      `scala-async`,
      ScalablyTyped.R.rxjs
    ),
    scalaVersion := "2.13.5",
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
      "-Xasync",
      "-Xcheckinit"
      //      "-Xasync" does not work with Scala.js js yet
    )
  )

def seleniumConfig2(base: String): SeleniumJSEnv.Config = {
  val contentDirName = "selenium"
  val webRoot        = s"http://localhost:9091/$contentDirName/"
  val contentDir     = s"$base/$contentDirName"
  SeleniumJSEnv
    .Config()
    .withMaterializeInServer(contentDir, webRoot)
}

lazy val `example-plain` = project
  .enablePlugins(ScalaJSPlugin)
  .settings(
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule).withSourceMap(false) },
    Test / jsEnv                    := {
      new SeleniumJSEnv(
        new ChromeOptions().setHeadless(true),
        seleniumConfig2((Compile / classDirectory).value.getParent)
      )
    },
    libraryDependencies ++= Seq(
      scalatest.value % Test,
      `scala-async`,
      ScalablyTyped.R.rxjs
    ),
    scalaVersion                    := "2.13.5",
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
      "-Xasync",
      "-Xcheckinit"
      //      "-Xasync" does not work with Scala.js js yet
    )
  )

lazy val `vite-project` = project
  .enablePlugins(ScalaJSPlugin)
  .settings(
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule).withSourceMap(false) },
    jsEnv                           := {
      new SeleniumJSEnv(
        {
          new ChromeOptions().setHeadless(true)
        },
        seleniumConfig
      )
    },
    libraryDependencies ++= Seq(
      scalatest.value % Test,
      `scala-async`,
      ScalablyTyped.R.rxjs
    ),
    scalaVersion                    := "2.13.5",
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
      "-Xasync",
      "-Xcheckinit"
      //      "-Xasync" does not work with Scala.js js yet
    )
  )

lazy val seleniumConfig: SeleniumJSEnv.Config = {
//  WebDriverManager.chromedriver().setup()
  val contentDirName = "vite-project-fastopt"
  val webRoot        = s"http://localhost:3000/$contentDirName/"
  val contentDir     = s"/Users/mushtaqahmed/projects/scala/sbt-snowpack/vite-project/target/scala-2.13/$contentDirName"
  SeleniumJSEnv
    .Config()
    .withMaterializeInServer(contentDir, webRoot)
}
