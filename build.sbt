import play.core.PlayVersion
import sbt.Tests.{Group, SubProcess}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import uk.gov.hmrc.SbtAutoBuildPlugin

import bloop.integrations.sbt.BloopDefaults

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    // Semicolon-separated list of regexs matching classes to exclude
    ScoverageKeys.coverageExcludedPackages := Seq(
      "<empty",
      """.*\.domain\.models\..*""" ,
      """uk\.gov\.hmrc\.apiplatformmicroservice\.common\.controllers\.binders""",
      """uk\.gov\.hmrc\.apiplatformmicroservice\.apidefinition\.controllers\.binders""",
      """uk\.gov\.hmrc\.BuildInfo""" ,
      """.*\.Routes""" ,
      """.*\.RoutesPrefix""" ,
      """.*Filters?""" ,
      """MicroserviceAuditConnector""" ,
      """Module""" ,
      """GraphiteStartUp""" ,
      """.*\.Reverse[^.]*""",
    ).mkString(";"),
    ScoverageKeys.coverageMinimum := 87.00,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    parallelExecution in Test := false
  )
}

lazy val compileDeps = Seq(
  "uk.gov.hmrc"       %% "bootstrap-play-26"         % "2.3.0",
  "com.beachape"      %% "enumeratum-play-json"      % "1.6.0",
  "org.typelevel"     %% "cats-core"                 % "2.0.0",
  "com.typesafe.play" %% "play-json"                 % "2.8.1",
  "com.typesafe.play" %% "play-json-joda"            % "2.8.1",
  "uk.gov.hmrc"       %% "play-json-union-formatter" % "1.11.0",
  "org.julienrf"      %% "play-json-derived-codecs"  % "6.0.0",
  "uk.gov.hmrc"       %% "json-encryption"           % "4.8.0-play-26",
  "uk.gov.hmrc"       %% "time"                      % "3.18.0"
)

def testDeps(scope: String) = Seq(
  "org.scalatestplus.play" %% "scalatestplus-play"        % "3.1.3",
  "org.mockito"            %% "mockito-scala-scalatest"   % "1.7.1",
  "com.typesafe.play"      %% "play-test"                 % PlayVersion.current,
  "org.pegdown"             % "pegdown"                   % "1.6.0",
  "com.github.tomakehurst"  % "wiremock-jre8-standalone"  % "2.27.1"
).map(_ % scope)

lazy val root = (project in file("."))
  .settings(
    name := "api-platform-microservice",
    organization := "uk.gov.hmrc",
    scalaVersion := "2.12.12",
    majorVersion := 0,
    PlayKeys.playDefaultPort := 6700,
    resolvers ++= Seq(
      Resolver.typesafeRepo("releases")
    ),
    libraryDependencies ++= compileDeps ++ testDeps("test,it"),
    publishingSettings,
    scoverageSettings,
    routesImport ++= Seq(
      "uk.gov.hmrc.apiplatformmicroservice.apidefinition.controllers.binders._",
      "uk.gov.hmrc.apiplatformmicroservice.common.controllers.binders._"
    )
  )
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
    Test / unmanagedSourceDirectories += baseDirectory.value / "testcommon",
    Test / unmanagedSourceDirectories += baseDirectory.value / "test"
  )
  .settings(scalacOptions ++= Seq("-Ypartial-unification"))
  .enablePlugins(PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)
  .disablePlugins(JUnitXmlReportPlugin)
