import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

object AppDependencies {
  val bootstrapVersion = "8.4.0"
  val apiDomainVersion = "0.13.0"
  val commonDomainVersion = "0.11.0"
  val appDomainVersion = "0.35.0"

  lazy val dependencies = Seq(
    caffeine,
    "uk.gov.hmrc"             %% "bootstrap-backend-play-30"         % bootstrapVersion,
    "org.julienrf"            %% "play-json-derived-codecs"          % "10.1.0",
    "uk.gov.hmrc"             %% "api-platform-api-domain"           % apiDomainVersion,
    "uk.gov.hmrc"             %% "api-platform-application-domain"   % appDomainVersion
  )

  lazy val testDependencies = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"           % bootstrapVersion,
    "org.pegdown"             %  "pegdown"                          % "1.6.0",
    "org.mockito"             %% "mockito-scala-scalatest"          % "1.17.29",
    "org.scalatest"           %% "scalatest"                        % "3.2.17",
    "uk.gov.hmrc"             %% "api-platform-test-common-domain"  % commonDomainVersion,
  ).map(_ % "test")
}
