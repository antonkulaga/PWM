import com.typesafe.sbt.packager.docker.{Cmd, DockerChmodType, ExecCmd}
import sbt.Keys._
import sbt._

name := "pwm"

organization := "com.github.antonkulaga"

scalaVersion :=  "2.13.6"

version := "0.0.2"

isSnapshot := false

scalacOptions ++= Seq("-feature", "-language:_", "-Ymacro-annotations")

javacOptions ++= Seq("-Xlint", "-J-Xss256M", "-encoding", "UTF-8")

javaOptions ++= Seq("-Xms512M", "-Xmx4096M", "-XX:MaxPermSize=2048M", "-XX:+CMSClassUnloadingEnabled")

Test / resourceDirectory := baseDirectory { _ / "files" }.value

Compile / unmanagedClasspath ++= (Compile / unmanagedResources).value

resolvers += Resolver.mavenLocal


resolvers += "jitpack.io" at "https://jitpack.io"

libraryDependencies ++= Seq(
 "org.scalanlp" %% "breeze" % "1.2",
 "org.wvlet.airframe" %% "airframe-log" % "21.7.0",
 "com.github.pathikrit" %% "better-files" % "3.9.1",
 "com.monovore" %% "decline" % "2.1.0",
 "com.github.antonkulaga" % "assembly" % "0.0.14",
 "org.scalatest" %% "scalatest-wordspec" % "3.2.9"  % Test,
 "org.scalatest" %% "scalatest-shouldmatchers" % "3.2.9" % Test,
 "org.scalatest" %% "scalatest-flatspec" % "3.2.9" % Test,
 "org.scalatest" %% "scalatest" % "3.2.9" % Test
)

Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-oF")

exportJars := true

run / fork := true

Test / parallelExecution := false


licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

dockerBaseImage := "ghcr.io/graalvm/graalvm-ce:latest"

//dockerBaseImage := "openjdk:11-oracle"

Docker / daemonUserUid := None

Docker / daemonUser := "root"

dockerExposedVolumes := Seq("/data")

dockerUpdateLatest := true

dockerChmodType := DockerChmodType.UserGroupWriteExecute

Docker / maintainer := "Anton Kulaga <antonkulaga@gmail.com>"

maintainer := "Anton Kulaga <antonkulaga@gmail.com>"

dockerRepository := Some("quay.io/antonkulaga")

dockerCommands ++= Seq(
 Cmd("WORKDIR", "/data"),
 Cmd("ENV", "JAVA_OPTS", "-Xmx4g")
)

enablePlugins(JavaAppPackaging, DockerPlugin)


