/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors

import javax.inject.{Inject, Named, Singleton}
import play.api.http.Status
import uk.gov.hmrc.apiplatformmicroservice.common.ProxiedHttpClient
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.Application
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.services.JsonFormatters._
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.ApiIdentifier
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.ThirdPartyApplicationConnector._
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.ThirdPartyApplicationConnector.JsonFormatters._

import uk.gov.hmrc.http._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.UpstreamErrorResponse.WithStatusCode
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models._

private[thirdpartyapplication] object ThirdPartyApplicationConnector {

  private[connectors] case class ApplicationResponse(id: ApplicationId)

  // N.B. This is a small subsection of the model that is normally returned
  private[connectors] case class InnerVersion(version: ApiVersion)
  private[connectors] case class SubscriptionVersion(version: InnerVersion, subscribed: Boolean)
  private[connectors] case class Subscription(context: ApiContext, versions: Seq[SubscriptionVersion])

  def asSetOfSubscriptions(input: Seq[Subscription]): Set[ApiIdentifier] = {
    input
      .flatMap(ss => ss.versions.map(vs => (ss.context, vs.version, vs.subscribed)))
      .filter(_._3)
      .map(t => ApiIdentifier(t._1, t._2.version))
      .toSet
  }

  private[connectors] object JsonFormatters {
    import play.api.libs.json._

    implicit val readsApplicationResponse = Json.reads[ApplicationResponse]
    implicit val readsInnerVersion = Json.reads[InnerVersion]
    implicit val readsSubscriptionVersion = Json.reads[SubscriptionVersion]
    implicit val readsSubscription = Json.reads[Subscription]
  }

  case class Config(
      applicationBaseUrl: String,
      applicationUseProxy: Boolean,
      applicationBearerToken: String,
      applicationApiKey: String)
}

private[thirdpartyapplication] abstract class ThirdPartyApplicationConnector(implicit val ec: ExecutionContext) {
  protected val httpClient: HttpClient
  protected val proxiedHttpClient: ProxiedHttpClient
  protected val config: ThirdPartyApplicationConnector.Config
  lazy val serviceBaseUrl: String = config.applicationBaseUrl

  // TODO Tidy this like Subs Fields to remove redundant "fixed" config for Principal connector
  lazy val useProxy: Boolean = config.applicationUseProxy
  lazy val bearerToken: String = config.applicationBearerToken
  lazy val apiKey: String = config.applicationApiKey

  def http: HttpClient = if (useProxy) proxiedHttpClient.withHeaders(bearerToken, apiKey) else httpClient

  def fetchApplication(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[Application]] = {
    http.GET[Option[Application]](s"$serviceBaseUrl/application/${applicationId.value}")
  }

  def fetchApplicationsByEmail(email: String)(implicit hc: HeaderCarrier): Future[Seq[ApplicationId]] = {
    http.GET[Seq[ApplicationResponse]](s"$serviceBaseUrl/application", Seq("emailAddress" -> email))
      .map(_.map(_.id))
  }

  def fetchSubscriptionsByEmail(email: String)(implicit hc: HeaderCarrier): Future[Seq[ApiIdentifier]] = {
    http.GET[Seq[ApiIdentifier]](s"$serviceBaseUrl/developer/$email/subscriptions")
  }

  class ApplicationNotFound extends RuntimeException

  def fetchSubscriptions(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Set[ApiIdentifier]] = {
    http.GET[Seq[Subscription]](s"$serviceBaseUrl/application/${applicationId.value}/subscription")
      .map(asSetOfSubscriptions)
      .recover {
        case Upstream5xxResponse(_, _, _, _)     => Set.empty // TODO - really mask this ?
        case WithStatusCode(Status.NOT_FOUND, _) => throw new ApplicationNotFound
      }
  }
}

@Singleton
private[thirdpartyapplication] class SubordinateThirdPartyApplicationConnector @Inject() (
    @Named("subordinate") override val config: ThirdPartyApplicationConnector.Config,
    override val httpClient: HttpClient,
    override val proxiedHttpClient: ProxiedHttpClient
  )(implicit override val ec: ExecutionContext)
    extends ThirdPartyApplicationConnector

@Singleton
private[thirdpartyapplication] class PrincipalThirdPartyApplicationConnector @Inject() (
    @Named("principal") override val config: ThirdPartyApplicationConnector.Config,
    override val httpClient: HttpClient,
    override val proxiedHttpClient: ProxiedHttpClient
  )(implicit override val ec: ExecutionContext)
    extends ThirdPartyApplicationConnector
