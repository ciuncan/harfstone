import Dependencies._

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
    libraryDependencies ++= (scalaTest +: zioTest).map(_ % Test),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
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
