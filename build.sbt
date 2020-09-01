import Dependencies._

Global / onChangedBuildSource := ReloadOnSourceChanges

inThisBuild(
  List(
    scalaVersion := "2.13.3",
    version := "0.1.0-SNAPSHOT",
    organization := "com.github.ciuncan",
    scalacOptions ++= Seq(
      "-encoding",
      "utf8",
      "-deprecation",
      "-unchecked",
      "-Xlint",
      "-Xfatal-warnings",
      "-feature",
      "-language:existentials",
      "-language:experimental.macros",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-Ymacro-annotations",
      // "-Ypartial-unification",
      "-Yrangepos"
    ),
    libraryDependencies ++= quicklens +: zio,
    libraryDependencies ++= (scalaTest +: scalaCheck +: zioTest).map(_ % Test),
    libraryDependencies ++= Seq(
      "org.specs2" %% "specs2-core"       % "4.10.3" % Test,
      "org.specs2" %% "specs2-scalacheck" % "4.10.3" % Test
    ),
    doctestTestFramework := DoctestTestFramework.Specs2,
    // doctestTestFramework := DoctestTestFramework.ScalaTest,
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    parallelExecution in Test := true,
    console / initialCommands += """
      |import ammonite.ops._
      |
      |import com.softwaremill.quicklens._
      |
      |import zio._
      |import zio.clock._
      |import zio.console._
      |import zio.duration._
      |import zio.Runtime.default._
      |
      |implicit class RunSyntax[A](io: ZIO[ZEnv, Any, A]) { def r: A = Runtime.default.unsafeRun(io.provideLayer(ZEnv.live)) }
      |
      |import com.github.ciuncan.harfstone._
      |import core.util.Implicits._
      |""".stripMargin
  )
)

lazy val root = (project in file("."))
  .settings(
    name := "harfstone",
    Ammonite / console := (`harfstone-core` / Ammonite / console).value
  )
  .aggregate(
    `harfstone-core`,
    `harfstone-console`
  )

lazy val `harfstone-core` = (project in file("./harfstone-core"))
  .settings(
    name := "harfstone-core"
  )

lazy val `harfstone-console` = (project in file("./harfstone-console"))
  .dependsOn(`harfstone-core` % "compile->compile;test->test")
  .settings(
    name := "harfstone-console",
    run / fork := true,
    run / connectInput := true,
    run / outputStrategy := Some(StdoutOutput)
  )
