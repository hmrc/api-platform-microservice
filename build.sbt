import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import uk.gov.hmrc.DefaultBuildSettings
import AppDependencies._

import bloop.integrations.sbt.BloopDefaults

lazy val appName = "api-platform-microservice"

scalaVersion := "2.13.8"

ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision

lazy val root = Project(appName, file("."))
  .settings(
    name := appName,
    organization := "uk.gov.hmrc",
    majorVersion := 0,
    PlayKeys.playDefaultPort := 6700,
    resolvers ++= Seq(
      Resolver.typesafeRepo("releases")
    ),
    libraryDependencies ++= dependencies ++ testDependencies,
    routesImport ++= Seq(
      "uk.gov.hmrc.apiplatformmicroservice.apidefinition.controllers.binders._",
      "uk.gov.hmrc.apiplatformmicroservice.common.controllers.binders._"
    )
  )
  .settings(ScoverageSettings())
  .configs(IntegrationTest)
  .settings(DefaultBuildSettings.integrationTestSettings())
  .settings(
    IntegrationTest / testOptions := Seq(Tests.Argument(TestFrameworks.ScalaTest, "-eT")),
    IntegrationTest / unmanagedSourceDirectories += baseDirectory.value / "testcommon",
    IntegrationTest / unmanagedSourceDirectories += baseDirectory.value / "it",
    IntegrationTest / parallelExecution := false
  )
  .settings(
    Test / fork := false,
    Test / parallelExecution := false,
    Test / testOptions := Seq(Tests.Argument(TestFrameworks.ScalaTest, "-eT")),
    Test / unmanagedSourceDirectories += baseDirectory.value / "testcommon",
    Test / unmanagedSourceDirectories += baseDirectory.value / "test"
  )
  .settings(
    scalacOptions ++= Seq(
      "-Wconf:cat=unused&src=views/.*\\.scala:s",
      "-Wconf:cat=unused&src=.*RoutesPrefix\\.scala:s",
      "-Wconf:cat=unused&src=.*Routes\\.scala:s",
      "-Wconf:cat=unused&src=.*ReverseRoutes\\.scala:s"
    )
  )
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)

Global / bloopAggregateSourceDependencies := true

commands ++= Seq(
  Command.command("run-all-tests") { state => "test" :: "it:test" :: state },

  Command.command("clean-and-test") { state => "clean" :: "compile" :: "run-all-tests" :: state },

  // Coverage does not need compile !
  Command.command("pre-commit") { state => "clean" :: "scalafmtAll" :: "scalafixAll" :: "coverage" :: "run-all-tests" :: "coverageReport" :: "coverageOff" :: state }
)