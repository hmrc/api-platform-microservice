import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

object AppDependencies {
  lazy val bootstrapVersion = "7.12.0"

  lazy val dependencies = Seq(
    caffeine,
    "uk.gov.hmrc"             %% "bootstrap-backend-play-28"         % bootstrapVersion,
    "com.beachape"            %% "enumeratum-play-json"              % "1.6.2",
    "org.julienrf"            %% "play-json-derived-codecs"          % "10.0.2",
    "uk.gov.hmrc"             %% "json-encryption"                   % "5.1.0-play-28",
    "uk.gov.hmrc"             %% "api-platform-api-domain"           % "0.9.0",
    "uk.gov.hmrc"             %% "api-platform-application-domain"   % "0.24.0"
  )

  lazy val testDependencies = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"     % bootstrapVersion,
    "org.pegdown"             %  "pegdown"                    % "1.6.0",
    "org.mockito"             %% "mockito-scala-scalatest"    % "1.17.29",
    "com.github.tomakehurst"  %  "wiremock-jre8-standalone"   % "2.27.1",
    "org.scalatest"           %% "scalatest"                  % "3.2.17"
  ).map(_ % "test,it")
}
