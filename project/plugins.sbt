addSbtPlugin("com.timushev.sbt" % "sbt-updates"  % "0.5.1")
addSbtPlugin("org.scalameta"    % "sbt-scalafmt" % "2.4.2")
addSbtPlugin("com.timushev.sbt" % "sbt-rewarn"   % "0.1.1")
addSbtPlugin("org.scala-js"     % "sbt-scalajs"  % "1.2.0")

libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value

resolvers += "jitpack" at "https://jitpack.io"
libraryDependencies += "com.github.mushtaq.scala-js-env-selenium" %% "scalajs-env-selenium" % "5374c6b"
libraryDependencies += "com.typesafe.play"                        %% "play-json"            % "2.9.1"

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

unmanagedSourceDirectories in Compile ++= {
  val root = baseDirectory.value.getParentFile
  Seq(root / "sbt-snowpack/src/main/scala")
}
