name := "pekko-quickstart-scala"

lazy val carSimulationProject = project
  .in(file("."))
  .settings(
    settings,
    libraryDependencies ++= Seq(
      library.logback,
      library.pekko,
      library.scalaTest,
      library.pekkoHttpTestKit
    )
  )

lazy val library = new {
  object version {
    val scala = "2.13.12"
    val pekkoVersion = "1.0.2"
    val logback = "1.2.13"
  }

  val logback = "ch.qos.logback" % "logback-classic" % version.logback

  val pekko =
    "org.apache.pekko" %% "pekko-actor-typed" % version.pekkoVersion

  val scalaTest = "org.scalatest" %% "scalatest" % "3.2.17" % Test
  val pekkoHttpTestKit =
    "org.apache.pekko" %% "pekko-actor-testkit-typed" % version.pekkoVersion % Test
}

lazy val settings =
  commonSettings

lazy val commonSettings = Seq(
  version := "1.0",
  scalaVersion := library.version.scala,
  fork := true
)
