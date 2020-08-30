import sbt._

object Dependencies {
  lazy val scalaTest  = "org.scalatest"     %% "scalatest"       % "3.1.2"
  lazy val scalaCheck = "org.scalatestplus" %% "scalacheck-1-14" % "3.1.2.0"

  lazy val quicklens = "com.softwaremill.quicklens" %% "quicklens" % "1.6.1"

  lazy val zio = Seq(
    "dev.zio" %% "zio"         % "1.0.0",
    "dev.zio" %% "zio-macros"  % "1.0.0",
    "dev.zio" %% "zio-streams" % "1.0.0"
  )

  lazy val zioTest = Seq(
    "dev.zio" %% "zio-test"          % "1.0.0",
    "dev.zio" %% "zio-test-sbt"      % "1.0.0",
    "dev.zio" %% "zio-test-magnolia" % "1.0.0"
  )
}
