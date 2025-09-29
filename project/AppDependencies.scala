import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

object AppDependencies {
  val bootstrapVersion = "9.19.0"
  val apiDomainVersion = "0.20.0"
  val appDomainVersion = "0.87.0"
  val tpdDomainVersion = "0.14.0"
  val mockitoScalaVersion = "2.0.0"

  lazy val dependencies = Seq(
    caffeine,
    "uk.gov.hmrc"             %% "bootstrap-backend-play-30"         % bootstrapVersion,
    "uk.gov.hmrc"             %% "api-platform-api-domain"           % apiDomainVersion,
    "uk.gov.hmrc"             %% "api-platform-application-domain"   % appDomainVersion,
    "uk.gov.hmrc"             %% "api-platform-tpd-domain"           % tpdDomainVersion
  )

  lazy val testDependencies = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"                     % bootstrapVersion,
    "org.mockito"             %% "mockito-scala-scalatest"                    % mockitoScalaVersion,
    "uk.gov.hmrc"             %% "api-platform-test-tpd-domain"               % tpdDomainVersion,
    "uk.gov.hmrc"             %% "api-platform-application-domain-fixtures"   % appDomainVersion

  ).map(_ % "test")
}
