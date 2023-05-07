import Dependencies._

ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.10"

lazy val db = (project in file("db"))
  .settings(
    name := "db",
    libraryDependencies ++= commonDependencies,
    resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
  )

lazy val app = (project in file("app"))
  .settings(
    name := "app",
    Compile / mainClass := Some("serv1.App"),
    Compile / unmanagedJars += file("libs/TwsApi.jar"),
    libraryDependencies ++= commonDependencies,
    resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
  ).dependsOn(db)

lazy val backend = (project in file("backend"))
  .settings(
    name := "backend",
    Compile / mainClass := Some("serv1.backend.BackendApp"),
    libraryDependencies ++= zioDependencies,
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  ).dependsOn(db)