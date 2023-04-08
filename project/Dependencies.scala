import sbt._


object Dependencies {
  val scalaMajorVersion = "2.13"
  val akkaHttpVersion = "10.2.9"
  val akkaVersion = "2.7.0"
  val swaggerVersion = "2.2.1"

  val commonDependencies: Seq[ModuleID] = Seq(
    // Use Scala 2.13 in our library project
    "org.scala-lang" % "scala-library" % "2.13.3",

    // This dependency is used by the application.
    "com.google.guava" % "guava" % "30.0-jre",

    // Use Scalatest for testing our library
    "junit" % "junit" % "4.13.1" % Test,
    "org.scalatest" % s"scalatest_$scalaMajorVersion" % "3.2.14" % Test,
    "org.scalamock" % s"scalamock_$scalaMajorVersion" % "5.1.0" % Test,
    "org.scalactic" % s"scalactic_$scalaMajorVersion" % "3.2.14" % Test,
    "org.scalatestplus" % s"junit-4-13_$scalaMajorVersion" % "3.2.2.0" % Test,
    "com.typesafe.akka" % s"akka-testkit_$scalaMajorVersion" % s"$akkaVersion" % Test,
    "com.typesafe.akka" % s"akka-actor-testkit-typed_$scalaMajorVersion" % s"$akkaVersion" % Test,

    // Need scala-xml at test runtime
    "org.scala-lang.modules" % "scala-xml_2.13" % "1.2.0" % Test,

    "com.typesafe.akka" % s"akka-http-bom_$scalaMajorVersion" % "10.2.9",

    "com.typesafe.akka" % s"akka-actor-typed_$scalaMajorVersion" % s"$akkaVersion",
    "com.typesafe.akka" % s"akka-stream_$scalaMajorVersion" % s"$akkaVersion",
    "com.typesafe.akka" % s"akka-stream-typed_$scalaMajorVersion" % s"$akkaVersion",
    "com.typesafe.akka" % s"akka-http_$scalaMajorVersion" % s"$akkaHttpVersion",
    "com.typesafe.akka" % s"akka-http-spray-json_$scalaMajorVersion" % s"$akkaHttpVersion",

    "jakarta.ws.rs" % "jakarta.ws.rs-api" % "3.1.0",

    "com.github.swagger-akka-http" % s"swagger-akka-http_$scalaMajorVersion" % "2.5.2",

    "com.typesafe.slick" % s"slick_$scalaMajorVersion" % "3.4.0",
    "com.typesafe.slick" % s"slick-hikaricp_$scalaMajorVersion" % "3.4.0",
    "com.typesafe.slick" % s"slick-codegen_$scalaMajorVersion" % "3.4.0",
    "org.postgresql" % "postgresql" % "42.5.1",

    "io.swagger.core.v3" % "swagger-core-jakarta" % s"$swaggerVersion",
    "io.swagger.core.v3" % "swagger-annotations-jakarta" % s"$swaggerVersion",
    "io.swagger.core.v3" % "swagger-models-jakarta" % s"$swaggerVersion",
    "io.swagger.core.v3" % "swagger-jaxrs2-jakarta" % s"$swaggerVersion",

    "com.typesafe.akka" % s"akka-slf4j_$scalaMajorVersion" % s"$akkaVersion",
    "ch.qos.logback" % "logback-classic" % "1.4.5",

    "org.snakeyaml" % "snakeyaml-engine" % "2.6-SNAPSHOT"
  )
}