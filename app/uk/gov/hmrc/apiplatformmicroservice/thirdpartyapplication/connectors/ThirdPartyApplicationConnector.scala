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
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps, UpstreamErrorResponse}

import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationWithCollaborators
import uk.gov.hmrc.apiplatform.modules.applications.core.interface.models.CreateApplicationRequestV2
import uk.gov.hmrc.apiplatformmicroservice.common.utils.EbridgeConfigurator
import uk.gov.hmrc.apiplatformmicroservice.common.{ApplicationLogger, EnvironmentAware}

private[thirdpartyapplication] object AbstractThirdPartyApplicationConnector {

  class ApplicationNotFound     extends RuntimeException
  class TeamMemberAlreadyExists extends RuntimeException("This user is already a teamMember on this application.")

  // N.B. This is a small subsection of the model that is normally returned
  private[connectors] case class InnerVersion(version: ApiVersionNbr)
  private[connectors] case class SubscriptionVersion(version: InnerVersion, subscribed: Boolean)
  private[connectors] case class Subscription(context: ApiContext, versions: Seq[SubscriptionVersion])

  private[connectors] object JsonFormatters {
    import play.api.libs.json._

    implicit val readsInnerVersion: Reads[InnerVersion]               = Json.reads[InnerVersion]
    implicit val readsSubscriptionVersion: Reads[SubscriptionVersion] = Json.reads[SubscriptionVersion]
    implicit val readsSubscription: Reads[Subscription]               = Json.reads[Subscription]
  }

  case class Config(
      applicationBaseUrl: String,
      applicationUseProxy: Boolean,
      applicationBearerToken: String,
      applicationApiKey: String
    )

}

trait ThirdPartyApplicationConnector {
  def fetchApplication(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[ApplicationWithCollaborators]]

  def fetchApplications(userId: UserId)(implicit hc: HeaderCarrier): Future[Seq[ApplicationId]]

  def fetchSubscriptions(userId: UserId)(implicit hc: HeaderCarrier): Future[Seq[ApiIdentifier]]

  def fetchSubscriptionsById(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Set[ApiIdentifier]]

  def createApplicationV2(createAppRequest: CreateApplicationRequestV2)(implicit hc: HeaderCarrier): Future[ApplicationId]

  // TODO API-8363
  // add methods currently in subs field connector
}

abstract private[thirdpartyapplication] class AbstractThirdPartyApplicationConnector(implicit val ec: ExecutionContext) extends ThirdPartyApplicationConnector
    with ApplicationLogger {

  import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.AbstractThirdPartyApplicationConnector._

  def http: HttpClientV2

  def configureEbridgeIfRequired: RequestBuilder => RequestBuilder

  protected val config: AbstractThirdPartyApplicationConnector.Config
  lazy val serviceBaseUrl: String = config.applicationBaseUrl

  def fetchApplication(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[ApplicationWithCollaborators]] = {
    configureEbridgeIfRequired(
      http.get(url"$serviceBaseUrl/application/${applicationId}")
    )
      .execute[Option[ApplicationWithCollaborators]]
  }

  def fetchApplications(userId: UserId)(implicit hc: HeaderCarrier): Future[Seq[ApplicationId]] = {
    configureEbridgeIfRequired(
      http.get(url"$serviceBaseUrl/developer/${userId}/applications")
    )
      .execute[Seq[ApplicationWithCollaborators]]
      .map(_.map(_.id))
  }

  def fetchSubscriptions(userId: UserId)(implicit hc: HeaderCarrier): Future[Seq[ApiIdentifier]] = {
    configureEbridgeIfRequired(
      http.get(url"$serviceBaseUrl/developer/${userId}/subscriptions")
    )
      .execute[Seq[ApiIdentifier]]
  }

  def fetchSubscriptionsById(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Set[ApiIdentifier]] = {
    configureEbridgeIfRequired(
      http.get(url"$serviceBaseUrl/application/${applicationId}/subscription")
    )
      .execute[Set[ApiIdentifier]]
      .recover {
        case UpstreamErrorResponse(_, NOT_FOUND, _, _) => throw new ApplicationNotFound
      }
  }

  def createApplicationV2(createAppRequest: CreateApplicationRequestV2)(implicit hc: HeaderCarrier): Future[ApplicationId] = {
    logger.info(s" Request to uplift ${createAppRequest.name} to production")
    configureEbridgeIfRequired(
      http
        .post(url"$serviceBaseUrl/application")
        .withBody(Json.toJson(createAppRequest))
    )
      .execute[ApplicationWithCollaborators]
      .map(_.id)
  }
}

@Singleton
@Named("subordinate")
class SubordinateThirdPartyApplicationConnector @Inject() (
    @Named("subordinate") override val config: AbstractThirdPartyApplicationConnector.Config,
    val http: HttpClientV2
  )(implicit override val ec: ExecutionContext
  ) extends AbstractThirdPartyApplicationConnector {

  lazy val useProxy: Boolean   = config.applicationUseProxy
  lazy val bearerToken: String = config.applicationBearerToken
  lazy val apiKey: String      = config.applicationApiKey

  lazy val configureEbridgeIfRequired: RequestBuilder => RequestBuilder =
    EbridgeConfigurator.configure(useProxy, bearerToken, apiKey)

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
    val http: HttpClientV2
  )(implicit override val ec: ExecutionContext
  ) extends AbstractThirdPartyApplicationConnector {

  val configureEbridgeIfRequired: RequestBuilder => RequestBuilder = identity

  def getLinkedSubordinateApplicationId(principalApplicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[ApplicationId]] = {
    http.get(url"$serviceBaseUrl/application/${principalApplicationId.value}/linked-subordinate-id")
      .execute[Option[ApplicationId]]
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
