resolvers += Classpaths.typesafeReleases

resolvers += Resolver.url("Typesafe repository - Releases", new java.net.URL("http://repo.typesafe.com/typesafe/releases/"))(Patterns(false, "[organisation]/[module]/[revision]/jars/[artifact].[ext]"))

addSbtPlugin("de.johoop" % "jacoco4sbt" % "2.1.2")

addSbtPlugin("com.openstudy" %% "sbt-resource-management" % "0.4.2")

resolvers += "sbt-idea-repo" at "https://oss.sonatype.org/content/repositories/releases/"

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.5.1")

addSbtPlugin("com.orrsella" % "sbt-sound" % "1.0.4")

addSbtPlugin("com.earldouglas" % "xsbt-web-plugin" % "0.9.0")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.11.2")

// Yui compressor (for resource management)
// This will move into the actual sbt-resource-management plugin at some point, but not quite yet.
libraryDependencies += "com.yahoo.platform.yui" % "yuicompressor"  % "2.4.7"

// Scala IDE plugin for Eclipse
addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "2.2.0")

addSbtPlugin("de.johoop" % "sbt-testng-plugin" % "3.0.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "0.7.1")

libraryDependencies += "com.typesafe" % "config" % "1.2.0"