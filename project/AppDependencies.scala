import play.core.PlayVersion
import sbt._

object AppDependencies {
  lazy val dependencies = Seq(
    "uk.gov.hmrc"             %% "bootstrap-play-26"          % "4.0.0",
    "com.beachape"            %% "enumeratum-play-json"       % "1.6.0",
    "org.typelevel"           %% "cats-core"                  % "2.0.0",
    "com.typesafe.play"       %% "play-json"                  % "2.8.1",
    "com.typesafe.play"       %% "play-json-joda"             % "2.8.1",
    "uk.gov.hmrc"             %% "play-json-union-formatter"  % "1.11.0",
    "org.julienrf"            %% "play-json-derived-codecs"   % "6.0.0",
    "uk.gov.hmrc"             %% "json-encryption"            % "4.8.0-play-26",
    "uk.gov.hmrc"             %% "time"                       % "3.18.0"
  )

  def testDependencies(scope: String) = Seq(
    "org.pegdown"             % "pegdown"                     % "1.6.0",
    "org.scalatestplus.play"  %% "scalatestplus-play"         % "3.1.3",
    "org.mockito"             %% "mockito-scala-scalatest"    % "1.7.1",
    "com.typesafe.play"       %% "play-test"                  % PlayVersion.current,
    "com.github.tomakehurst"  % "wiremock-jre8-standalone"    % "2.27.1"
  ).map(_ % scope)
}
