import sbt.Def.{setting => dep}
import sbt._
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._

object Libs {
  // 1.0.0-M1 does not work with Scala.js js yet
  val `scala-async` = "org.scala-lang.modules" %% "scala-async" % "1.0.1"
  val scalatest     = dep("org.scalatest" %%% "scalatest" % "3.2.10")
}
