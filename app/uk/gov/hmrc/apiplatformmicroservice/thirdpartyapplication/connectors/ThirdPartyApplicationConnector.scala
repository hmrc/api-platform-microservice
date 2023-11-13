/*
 * Copyright 2023 HM Revenue & Customs
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
import scala.concurrent.{ExecutionContext, Future}

import play.api.http.Status._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HttpClient, _}

import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.applications.core.interface.models.{CreateApplicationRequestV1, CreateApplicationRequestV2}
import uk.gov.hmrc.apiplatformmicroservice.common.{ApplicationLogger, EnvironmentAware, ProxiedHttpClient}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.Application
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.services.ApplicationJsonFormatters._

private[thirdpartyapplication] object AbstractThirdPartyApplicationConnector {

  class ApplicationNotFound     extends RuntimeException
  class TeamMemberAlreadyExists extends RuntimeException("This user is already a teamMember on this application.")

  private[connectors] case class ApplicationResponse(id: ApplicationId)

  // N.B. This is a small subsection of the model that is normally returned
  private[connectors] case class InnerVersion(version: ApiVersionNbr)
  private[connectors] case class SubscriptionVersion(version: InnerVersion, subscribed: Boolean)
  private[connectors] case class Subscription(context: ApiContext, versions: Seq[SubscriptionVersion])

  private[connectors] object JsonFormatters {
    import play.api.libs.json._

    implicit val readsApplicationResponse = Json.reads[ApplicationResponse]
    implicit val readsInnerVersion        = Json.reads[InnerVersion]
    implicit val readsSubscriptionVersion = Json.reads[SubscriptionVersion]
    implicit val readsSubscription        = Json.reads[Subscription]
  }

  case class Config(
      applicationBaseUrl: String,
      applicationUseProxy: Boolean,
      applicationBearerToken: String,
      applicationApiKey: String
    )

}

trait ThirdPartyApplicationConnector {
  def fetchApplication(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[Application]]

  def fetchApplications(userId: UserId)(implicit hc: HeaderCarrier): Future[Seq[ApplicationId]]

  def fetchSubscriptions(userId: UserId)(implicit hc: HeaderCarrier): Future[Seq[ApiIdentifier]]

  def fetchSubscriptionsById(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Set[ApiIdentifier]]

  def createApplicationV1(createAppRequest: CreateApplicationRequestV1)(implicit hc: HeaderCarrier): Future[ApplicationId]

  def createApplicationV2(createAppRequest: CreateApplicationRequestV2)(implicit hc: HeaderCarrier): Future[ApplicationId]

}

abstract private[thirdpartyapplication] class AbstractThirdPartyApplicationConnector(implicit val ec: ExecutionContext) extends ThirdPartyApplicationConnector
    with ApplicationLogger {

  import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.AbstractThirdPartyApplicationConnector._
  import AbstractThirdPartyApplicationConnector.JsonFormatters._

  protected val httpClient: HttpClient
  protected val proxiedHttpClient: ProxiedHttpClient
  protected val config: AbstractThirdPartyApplicationConnector.Config
  lazy val serviceBaseUrl: String = config.applicationBaseUrl

  // TODO Tidy this like Subs Fields to remove redundant "fixed" config for Principal connector
  lazy val useProxy: Boolean   = config.applicationUseProxy
  lazy val bearerToken: String = config.applicationBearerToken
  lazy val apiKey: String      = config.applicationApiKey

  def http: HttpClient = if (useProxy) proxiedHttpClient.withHeaders(bearerToken, apiKey) else httpClient

  def fetchApplication(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[Application]] = {
    http.GET[Option[Application]](s"$serviceBaseUrl/application/${applicationId}")
  }

  def fetchApplications(userId: UserId)(implicit hc: HeaderCarrier): Future[Seq[ApplicationId]] = {
    http.GET[Seq[ApplicationResponse]](s"$serviceBaseUrl/developer/${userId}/applications").map(_.map(_.id))
  }

  def fetchSubscriptions(userId: UserId)(implicit hc: HeaderCarrier): Future[Seq[ApiIdentifier]] = {
    http.GET[Seq[ApiIdentifier]](s"$serviceBaseUrl/developer/${userId}/subscriptions")
  }

  def fetchSubscriptionsById(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Set[ApiIdentifier]] = {
    http.GET[Set[ApiIdentifier]](s"$serviceBaseUrl/application/${applicationId}/subscription")
      .recover {
        case UpstreamErrorResponse(_, NOT_FOUND, _, _) => throw new ApplicationNotFound
      }
  }

  def createApplicationV1(createAppRequest: CreateApplicationRequestV1)(implicit hc: HeaderCarrier): Future[ApplicationId] = {
    logger.info(s" Request to uplift ${createAppRequest.name} to production")
    http.POST[CreateApplicationRequestV1, ApplicationResponse](s"$serviceBaseUrl/application", createAppRequest).map(_.id)
  }

  def createApplicationV2(createAppRequest: CreateApplicationRequestV2)(implicit hc: HeaderCarrier): Future[ApplicationId] = {
    logger.info(s" Request to uplift ${createAppRequest.name} to production")
    http.POST[CreateApplicationRequestV2, ApplicationResponse](s"$serviceBaseUrl/application", createAppRequest).map(_.id)
  }
}

@Singleton
@Named("subordinate")
class SubordinateThirdPartyApplicationConnector @Inject() (
    @Named("subordinate") override val config: AbstractThirdPartyApplicationConnector.Config,
    override val httpClient: HttpClient,
    override val proxiedHttpClient: ProxiedHttpClient
  )(implicit override val ec: ExecutionContext
  ) extends AbstractThirdPartyApplicationConnector {

  override def fetchSubscriptionsById(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Set[ApiIdentifier]] = {
    super.fetchSubscriptionsById(applicationId)
      .recover {
        case UpstreamErrorResponse.Upstream5xxResponse(_) => Set.empty // TODO - really mask this ?
      }
  }
}

@Singleton
@Named("principal")
class PrincipalThirdPartyApplicationConnector @Inject() (
    @Named("principal") override val config: AbstractThirdPartyApplicationConnector.Config,
    override val httpClient: HttpClient,
    override val proxiedHttpClient: ProxiedHttpClient
  )(implicit override val ec: ExecutionContext
  ) extends AbstractThirdPartyApplicationConnector {

  def getLinkedSubordinateApplicationId(principalApplicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[ApplicationId]] = {
    http.GET[Option[ApplicationId]](s"$serviceBaseUrl/application/${principalApplicationId.value}/linked-subordinate-id")
  }

}

@Singleton
class EnvironmentAwareThirdPartyApplicationConnector @Inject() (
    @Named("subordinate") val subordinate: ThirdPartyApplicationConnector,
    @Named("principal") val principal: ThirdPartyApplicationConnector
  ) extends EnvironmentAware[ThirdPartyApplicationConnector]

sealed trait AddCollaboratorResult
case class AddCollaboratorSuccessResult(userRegistered: Boolean) extends AddCollaboratorResult
case object CollaboratorAlreadyExistsFailureResult               extends AddCollaboratorResult
