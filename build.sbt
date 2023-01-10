import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import uk.gov.hmrc.SbtAutoBuildPlugin
import AppDependencies._

import bloop.integrations.sbt.BloopDefaults

lazy val appName = "api-platform-microservice"

lazy val root = Project(appName, file("."))
  .settings(
    name := appName,
    organization := "uk.gov.hmrc",
    scalaVersion := "2.12.12",
    majorVersion := 0,
    PlayKeys.playDefaultPort := 6700,
    resolvers ++= Seq(
      Resolver.typesafeRepo("releases")
    ),
    libraryDependencies ++= dependencies ++ testDependencies,
    publishingSettings,
    routesImport ++= Seq(
      "uk.gov.hmrc.apiplatformmicroservice.apidefinition.controllers.binders._",
      "uk.gov.hmrc.apiplatformmicroservice.common.controllers.binders._"
    )
  )
  .settings(ScoverageSettings())
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(inConfig(IntegrationTest)(BloopDefaults.configSettings))
  .settings(
    IntegrationTest / testOptions := Seq(Tests.Argument(TestFrameworks.ScalaTest, "-eT")),
    IntegrationTest / unmanagedSourceDirectories += baseDirectory.value / "testcommon",
    IntegrationTest / unmanagedSourceDirectories += baseDirectory.value / "it", 
    IntegrationTest / parallelExecution := false
  )
  .settings(inConfig(Test)(BloopDefaults.configSettings))
  .settings(
    Test / fork := false,
    Test / parallelExecution := false,
    Test / testOptions := Seq(Tests.Argument(TestFrameworks.ScalaTest, "-eT")),
    Test / unmanagedSourceDirectories += baseDirectory.value / "testcommon",
    Test / unmanagedSourceDirectories += baseDirectory.value / "test"
  )
  .settings(scalacOptions ++= Seq("-Ypartial-unification"))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)
