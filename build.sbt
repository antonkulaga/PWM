import sbt.Keys._

import sbt._

name := "PWM"

organization := "group.aging-research"

scalaVersion :=  "2.12.7"

version := "0.0.1"

coursierMaxIterations := 200

isSnapshot := false

scalacOptions ++= Seq( "-target:jvm-1.8", "-feature", "-language:_" )

javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint", "-J-Xss5M", "-encoding", "UTF-8")

javaOptions ++= Seq("-Xms512M", "-Xmx2048M", "-XX:MaxPermSize=2048M", "-XX:+CMSClassUnloadingEnabled")

resourceDirectory in Test := baseDirectory { _ / "files" }.value

unmanagedClasspath in Compile ++= (unmanagedResources in Compile).value

resolvers += Resolver.mavenLocal

resolvers += Resolver.sonatypeRepo("releases")

addCompilerPlugin(("org.scalamacros" %% "paradise" % "2.1.0").cross(CrossVersion.full))

libraryDependencies ++= Seq(
 "org.typelevel" %% "cats-core" % "1.4.0",
 "org.scalanlp" %% "breeze" % "1.0-RC2",
 "org.scalanlp" %% "breeze-natives" % "1.0-RC2",
 "com.github.pathikrit" %% "better-files" % "3.6.0",
 "com.monovore" %% "decline" % "0.5.1",
 "org.scalatest" %% "scalatest" % "3.0.5"
)

testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oF")

exportJars := true

fork in run := true

parallelExecution in Test := false

bintrayRepository := "main"

bintrayOrganization := Some("comp-bio-aging")

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

maintainer in Docker := "Anton Kulaga <antonkulaga@gmail.com>"

maintainer := "Anton Kulaga <antonkulaga@gmail.com>"

dockerRepository := Some("quay.io/comp-bio-aging")


enablePlugins(JavaAppPackaging, DockerPlugin)


