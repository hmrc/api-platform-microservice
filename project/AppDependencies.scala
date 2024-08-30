import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

object AppDependencies {
  val bootstrapVersion = "9.3.0"
  val apiDomainVersion = "0.17.0"  // 0.17.0 uses common-domain 0.13.0
  val commonDomainVersion = "0.13.0"
  val appDomainVersion = "0.57.0"
  val tpdDomainVersion = "0.2.0"

  lazy val dependencies = Seq(
    caffeine,
    "uk.gov.hmrc"             %% "bootstrap-backend-play-30"         % bootstrapVersion,
    "uk.gov.hmrc"             %% "api-platform-api-domain"           % apiDomainVersion,
    "uk.gov.hmrc"             %% "api-platform-application-domain"   % appDomainVersion,
    "uk.gov.hmrc"             %% "api-platform-tpd-domain"           % tpdDomainVersion
  )

  lazy val testDependencies = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"           % bootstrapVersion,
    "org.pegdown"             %  "pegdown"                          % "1.6.0",
    "org.mockito"             %% "mockito-scala-scalatest"          % "1.17.29",
    "org.scalatest"           %% "scalatest"                        % "3.2.17",
    "uk.gov.hmrc"             %% "api-platform-test-tpd-domain"     % tpdDomainVersion,
  ).map(_ % "test")
}
