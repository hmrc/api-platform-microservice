import play.core.PlayVersion
import sbt.Tests.{Group, SubProcess}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import uk.gov.hmrc.SbtAutoBuildPlugin

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    // Semicolon-separated list of regexs matching classes to exclude
    ScoverageKeys.coverageExcludedPackages := """uk\.gov\.hmrc\.BuildInfo;.*\.Routes;.*\.RoutesPrefix;.*Filters?;MicroserviceAuditConnector;Module;GraphiteStartUp;.*\.Reverse[^.]*""",
    ScoverageKeys.coverageMinimum := 90.00,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    parallelExecution in Test := false
  )
}

lazy val compileDeps = Seq(
  "uk.gov.hmrc"       %% "bootstrap-play-26"         % "1.13.0",
  "com.beachape"      %% "enumeratum-play-json"      % "1.6.0",
  "org.typelevel"     %% "cats-core"                 % "2.1.0",
  "com.typesafe.play" %% "play-json"                 % "2.8.1",
  "com.typesafe.play" %% "play-json-joda"            % "2.8.1",
  "uk.gov.hmrc"       %% "play-json-union-formatter" % "1.11.0"
)

def testDeps(scope: String) = Seq(
  "uk.gov.hmrc"            %% "hmrctest"                % "3.9.0-play-26"     % scope,
  "org.scalatestplus.play" %% "scalatestplus-play"      % "3.1.3"             % scope,
  "org.mockito"            %% "mockito-scala-scalatest" % "1.14.8"            % scope,
  "com.typesafe.play"      %% "play-test"               % PlayVersion.current % scope
)

lazy val root = (project in file("."))
  .settings(
    name := "api-platform-microservice",
    organization := "uk.gov.hmrc",
    scalaVersion := "2.12.10",
    majorVersion := 0,
    PlayKeys.playDefaultPort := 6700,
    resolvers := Seq(
      Resolver.bintrayRepo("hmrc", "releases"),
      Resolver.bintrayRepo("hmrc", "release-candidates"),
      Resolver.typesafeRepo("releases"),
      Resolver.jcenterRepo
    ),
    libraryDependencies ++= compileDeps ++ testDeps("test"),
    publishingSettings,
    scoverageSettings
  )
  .settings(scalacOptions ++= Seq("-Ypartial-unification"))
  .enablePlugins(PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)
  .disablePlugins(JUnitXmlReportPlugin)

def oneForkedJvmPerTest(tests: Seq[TestDefinition]) = {
  tests.map { test =>
    new Group(test.name, Seq(test), SubProcess(ForkOptions().withRunJVMOptions(Vector(s"-Dtest.name=${test.name}"))))
  }
}
