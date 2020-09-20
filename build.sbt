import scala.xml.Elem

inThisBuild(
  Seq(
    scalaVersion := "2.12.12",
    version := "0.1.0-SNAPSHOT",
    organization := "com.github.mushtaq.scalajs-selenium-snowpack",
    organizationName := "ThoughtWorks",
    resolvers += "jitpack" at "https://jitpack.io",
    scalafmtOnCompile := true,
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
    licenses := Seq(
      ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))
    )
  )
)

lazy val `scalajs-selenium-snowpack-root` = project
  .in(file("."))
  .aggregate(`scalajs-selenium-snowpack`)

lazy val `scalajs-selenium-snowpack` = project
  .enablePlugins(ScriptedPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "com.github.mushtaq.scala-js-env-selenium" %% "scalajs-env-selenium" % "1a06087"
    ),
    scriptedLaunchOpts += ("-Dplugin.version=" + version.value),
    scriptedLaunchOpts ++= sys.process.javaVmArguments.filter(a => Seq("-Xmx", "-Xms", "-XX", "-Dsbt.log.noformat").exists(a.startsWith)),
    scriptedBufferLog := false,
    sbtPlugin := true,
    publishMavenStyle := true,
    publishM2 := {
      publishM2.value

      val d = file(
        sys.env("HOME")
      ) / s".m2/repository/com/github/mushtaq/scalajs-selenium-snowpack/scalajs-selenium-snowpack_${scalaBinaryVersion.value}_${sbtBinaryVersion.value}"
      d.renameTo(
        file(
          sys.env("HOME")
        ) / s".m2/repository/com/github/mushtaq/scalajs-selenium-snowpack/scalajs-selenium-snowpack_${scalaBinaryVersion.value}"
      )
    },
    pomPostProcess := { input =>
      val newArtifactId = <artifactId>{name.value}_{scalaBinaryVersion.value}</artifactId>
      input match {
        case elem: Elem =>
          val updatedChild = input.child.map {
            case elem: Elem if elem.label == "artifactId" => newArtifactId
            case x                                        => x
          }
          elem.copy(child = updatedChild)
        case x          => x
      }
    }
  )
