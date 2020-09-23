import sbt.Def.{setting => dep}
import sbt._
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._

object Libs {
  // 1.0.0-M1 does not work with Scala.js js yet
  val `scala-async` = "org.scala-lang.modules" %% "scala-async" % "0.10.0"

  val scalatest     = dep("org.scalatest" %%% "scalatest" % "3.2.2")
  val `scalajs-dom` = dep("org.scala-js" %%% "scalajs-dom" % "1.1.0")
}
