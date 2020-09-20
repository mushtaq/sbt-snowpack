sys.props.get("plugin.version") match {
  case Some(x) => addSbtPlugin("com.github.mushtaq" % "scalajs-selenium-snowpack" % x)
  case _       => sys.error("""|The system property 'plugin.version' is not defined.
                         |Specify this property using the scriptedLaunchOpts -D.""".stripMargin)
}

resolvers += "jitpack" at "https://jitpack.io"

resolvers += Resolver.bintrayRepo("oyvindberg", "ScalablyTyped")
addSbtPlugin("org.scalablytyped" % "sbt-scalablytyped" % "202008250800")
