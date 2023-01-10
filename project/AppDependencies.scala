import play.core.PlayVersion
import sbt._

object AppDependencies {
  lazy val bootstrapVersion = "7.12.0"
  
  lazy val dependencies = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-28"  % bootstrapVersion,
    "com.beachape"            %% "enumeratum-play-json"       % "1.6.0",
    "org.typelevel"           %% "cats-core"                  % "2.0.0",
    "com.typesafe.play"       %% "play-json"                  % "2.9.2",
    "com.typesafe.play"       %% "play-json-joda"             % "2.9.2",
    "uk.gov.hmrc"             %% "play-json-union-formatter"  % "1.17.0-play-28",
    "org.julienrf"            %% "play-json-derived-codecs"   % "7.0.0",
    "uk.gov.hmrc"             %% "json-encryption"            % "5.1.0-play-28",
    "uk.gov.hmrc"             %% "time"                       % "3.25.0"
  )

  lazy val testDependencies = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"     % bootstrapVersion,
    "org.pegdown"             %  "pegdown"                    % "1.6.0",
    "org.mockito"             %% "mockito-scala-scalatest"    % "1.7.1",
    "com.github.tomakehurst"  %  "wiremock-jre8-standalone"   % "2.27.1"
  ).map(_ % "test,it")
}
