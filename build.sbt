import Dependencies._

ThisBuild / scalaVersion := "2.13.3"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.github.ciuncan"
ThisBuild / scalacOptions ++= Seq(
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
)
ThisBuild / libraryDependencies ++= quicklens +: zio
ThisBuild / libraryDependencies ++= (scalaTest +: zioTest).map(_ % Test)
ThisBuild / testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")

lazy val `harfstone-core` = (project in file("./harfstone-core"))
  .settings(
    name := "harfstone-core"
  )

lazy val `harfstone-console` = (project in file("./harfstone-console"))
  .dependsOn(`harfstone-core` % "compile->compile;test->test")
  .settings(
    name := "harfstone-console",
    fork in run := true,
    logBuffered in run := false,
    connectInput in run := true
  )
