import com.typesafe.sbt.packager.docker.{Cmd, DockerChmodType, ExecCmd}
import sbt.Keys._
import sbt._

name := "pwm"

organization := "group.aging-research"

scalaVersion :=  "2.13.1"

version := "0.0.16"

isSnapshot := false

scalacOptions ++= Seq("-feature", "-language:_", "-Ymacro-annotations")

javacOptions ++= Seq("-Xlint", "-J-Xss256M", "-encoding", "UTF-8")

javaOptions ++= Seq("-Xms512M", "-Xmx4096M", "-XX:MaxPermSize=2048M", "-XX:+CMSClassUnloadingEnabled")

resourceDirectory in Test := baseDirectory { _ / "files" }.value

unmanagedClasspath in Compile ++= (unmanagedResources in Compile).value

resolvers += Resolver.mavenLocal

resolvers += Resolver.sonatypeRepo("releases")

resolvers += Resolver.bintrayRepo("comp-bio-aging", "main")

addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")

libraryDependencies ++= Seq(
 "org.scalanlp" %% "breeze" % "1.0",
 "org.scalanlp" %% "breeze-natives" % "1.0",
 "org.wvlet.airframe" %% "airframe-log" % "19.11.1",
 "com.github.pathikrit" %% "better-files" % "3.8.0",
 "com.monovore" %% "decline" % "1.0.0",
 "com.monovore" %% "decline-refined" % "1.0.0",
 "group.aging-research" %% "assembly" % "0.0.13",
 "org.scalatest" %% "scalatest" % "3.0.8" % Test
)

testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oF")

exportJars := true

fork in run := true

parallelExecution in Test := false

bintrayRepository := "main"

bintrayOrganization := Some("comp-bio-aging")

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

dockerBaseImage := "oracle/graalvm-ce:19.2.1"

//dockerBaseImage := "openjdk:11-oracle"

daemonUserUid in Docker := None

daemonUser in Docker := "root"

dockerExposedVolumes := Seq("/data")

dockerUpdateLatest := true

dockerChmodType := DockerChmodType.UserGroupWriteExecute

maintainer in Docker := "Anton Kulaga <antonkulaga@gmail.com>"

maintainer := "Anton Kulaga <antonkulaga@gmail.com>"

dockerRepository := Some("quay.io/comp-bio-aging")

dockerCommands ++= Seq(
 Cmd("RUN","yum install -y openblas.x86_64 openblas-devel"),
 Cmd("WORKDIR", "/data"),
 Cmd("ENV", "JAVA_OPTS", "-Xmx4g")
)

enablePlugins(JavaAppPackaging, DockerPlugin)


