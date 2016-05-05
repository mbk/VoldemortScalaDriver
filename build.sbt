name := "Simple Voldemort driver template"

version := "0.9.0"

organization := "net.vrijheid"

scalaVersion := "2.11.8"

resolvers ++= Seq("snapshots"     at "http://oss.sonatype.org/content/repositories/snapshots",
                "releases"        at "http://oss.sonatype.org/content/repositories/releases"
                )

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature")



