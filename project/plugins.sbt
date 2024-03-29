addSbtPlugin("com.timushev.sbt" % "sbt-updates"  % "0.6.0")
addSbtPlugin("org.scalameta"    % "sbt-scalafmt" % "2.4.3")
addSbtPlugin("com.timushev.sbt" % "sbt-rewarn"   % "0.1.3")

libraryDependencies += "org.scala-sbt"       %% "scripted-plugin"  % sbtVersion.value
libraryDependencies += "io.github.bonigarcia" % "webdrivermanager" % "5.0.3"

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

resolvers += Resolver.bintrayRepo("oyvindberg", "ScalablyTyped")
addSbtPlugin("org.scalablytyped" % "sbt-scalablytyped" % "202008250800")

////////////////////

// this is a source dependency on plugin to enable recursion, see comments in the build.sbt
unmanagedSourceDirectories in Compile ++= {
  val root = baseDirectory.value.getParentFile
  Seq(root / "sbt-snowpack/src/main/scala")
}

// below settings are duplicated in both build.sbt and plugins.sbt, see comment in build.sbt
libraryDependencies += "com.typesafe.play" %% "play-json"            % "2.9.2"
libraryDependencies += "org.scala-js"      %% "scalajs-env-selenium" % "1.1.1"
// note, 'sbt-scalajs' must come after 'scalajs-env-selenium'
// reference: https://github.com/scala-js/scala-js-env-selenium#usage
addSbtPlugin("org.scala-js"                 % "sbt-scalajs"          % "1.7.1")
