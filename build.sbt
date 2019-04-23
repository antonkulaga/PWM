import com.typesafe.sbt.packager.docker.{Cmd, DockerChmodType}
import sbt.Keys._
import sbt._

name := "pwm"

organization := "group.aging-research"

scalaVersion :=  "2.12.8"

version := "0.0.13"

coursierMaxIterations := 200

isSnapshot := false

javacOptions ++= Seq("-Xlint", "-J-Xss5M", "-encoding", "UTF-8")

javaOptions ++= Seq("-Xms512M", "-Xmx4096M", "-XX:MaxPermSize=2048M", "-XX:+CMSClassUnloadingEnabled")

resourceDirectory in Test := baseDirectory { _ / "files" }.value

unmanagedClasspath in Compile ++= (unmanagedResources in Compile).value

resolvers += Resolver.mavenLocal

resolvers += Resolver.sonatypeRepo("releases")

resolvers += Resolver.bintrayRepo("comp-bio-aging", "main")

addCompilerPlugin(("org.scalamacros" %% "paradise" % "2.1.1").cross(CrossVersion.full))

addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.0")

libraryDependencies ++= Seq(
 "org.scalanlp" %% "breeze" % "1.0-RC2",
 "org.scalanlp" %% "breeze-natives" % "1.0-RC2",
 "org.wvlet.airframe" %% "airframe-log" % "19.4.1",
 "com.github.pathikrit" %% "better-files" % "3.7.1",
 "com.monovore" %% "decline" % "0.6.2",
 "com.monovore" %% "decline-refined" % "0.6.2",
 "group.aging-research" %% "assembly" % "0.0.10.1",
 "org.scalatest" %% "scalatest" % "3.0.7" % Test
)

testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oF")

exportJars := true

fork in run := true

parallelExecution in Test := false

bintrayRepository := "main"

bintrayOrganization := Some("comp-bio-aging")

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

dockerBaseImage := "openjdk:11-oracle"

daemonUserUid in Docker := None

daemonUser in Docker := "root"

dockerExposedVolumes := Seq("/data")

dockerUpdateLatest := true

dockerChmodType := DockerChmodType.UserGroupWriteExecute

maintainer in Docker := "Anton Kulaga <antonkulaga@gmail.com>"

maintainer := "Anton Kulaga <antonkulaga@gmail.com>"

dockerRepository := Some("quay.io/comp-bio-aging")

dockerCommands ++= Seq(
 Cmd("WORKDIR", "/data")
)

enablePlugins(JavaAppPackaging, DockerPlugin)


