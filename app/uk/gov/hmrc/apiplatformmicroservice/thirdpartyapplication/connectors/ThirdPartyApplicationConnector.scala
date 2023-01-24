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

import uk.gov.hmrc.apiplatform.modules.apis.domain.models.{ApiContext, ApiIdentifier, ApiVersion}
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.developers.domain.models.UserId
import uk.gov.hmrc.apiplatformmicroservice.common.{ApplicationLogger, EnvironmentAware, ProxiedHttpClient}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.domain.{AddCollaboratorToTpaRequest, AddCollaboratorToTpaResponse}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.{Application, _}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.services.ApplicationJsonFormatters._

private[thirdpartyapplication] object AbstractThirdPartyApplicationConnector {

  class ApplicationNotFound     extends RuntimeException
  class TeamMemberAlreadyExists extends RuntimeException("This user is already a teamMember on this application.")

  private[connectors] case class ApplicationResponse(id: ApplicationId)

  // N.B. This is a small subsection of the model that is normally returned
  private[connectors] case class InnerVersion(version: ApiVersion)
  private[connectors] case class SubscriptionVersion(version: InnerVersion, subscribed: Boolean)
  private[connectors] case class Subscription(context: ApiContext, versions: Seq[SubscriptionVersion])

  private[connectors] object JsonFormatters {
    import play.api.libs.json._

    implicit val readsApiIdentifier       = Json.reads[ApiIdentifier]
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

  def updateApplication(applicationId: ApplicationId, applicationUpdate: ApplicationUpdate)(implicit hc: HeaderCarrier): Future[Application]

  @deprecated("remove after clients are no longer using the old endpoint")
  def subscribeToApi(applicationId: ApplicationId, apiIdentifier: ApiIdentifier)(implicit hc: HeaderCarrier): Future[SubscriptionUpdateResult]

  @deprecated("remove after clients are no longer using the old endpoint")
  def addCollaborator(applicationId: ApplicationId, addCollaboratorRequest: AddCollaboratorToTpaRequest)(implicit hc: HeaderCarrier): Future[AddCollaboratorResult]

  def createApplicationV1(createAppRequest: CreateApplicationRequestV1)(implicit hc: HeaderCarrier): Future[ApplicationId]

  def createApplicationV2(createAppRequest: CreateApplicationRequestV2)(implicit hc: HeaderCarrier): Future[ApplicationId]

}

private[thirdpartyapplication] abstract class AbstractThirdPartyApplicationConnector(implicit val ec: ExecutionContext) extends ThirdPartyApplicationConnector
    with ApplicationUpdateFormatters with ApplicationLogger {

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
    http.GET[Option[Application]](s"$serviceBaseUrl/application/${applicationId.value}")
  }

  def fetchApplications(userId: UserId)(implicit hc: HeaderCarrier): Future[Seq[ApplicationId]] = {
    http.GET[Seq[ApplicationResponse]](s"$serviceBaseUrl/developer/${userId.value}/applications").map(_.map(_.id))
  }

  def fetchSubscriptions(userId: UserId)(implicit hc: HeaderCarrier): Future[Seq[ApiIdentifier]] = {
    http.GET[Seq[ApiIdentifier]](s"$serviceBaseUrl/developer/${userId.value}/subscriptions")
  }

  def fetchSubscriptionsById(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Set[ApiIdentifier]] = {
    http.GET[Set[ApiIdentifier]](s"$serviceBaseUrl/application/${applicationId.value}/subscription")
      .recover {
        case UpstreamErrorResponse(_, NOT_FOUND, _, _) => throw new ApplicationNotFound
      }
  }

  def updateApplication(applicationId: ApplicationId, applicationUpdate: ApplicationUpdate)(implicit hc: HeaderCarrier): Future[Application] = {
    http.PATCH[ApplicationUpdate, Application](
      s"$serviceBaseUrl/application/${applicationId.value}",
      applicationUpdate
    )
      .recover {
        case UpstreamErrorResponse(_, NOT_FOUND, _, _) => throw new ApplicationNotFound
      }
  }

  @deprecated("remove after clients are no longer using the old endpoint")
  def subscribeToApi(applicationId: ApplicationId, apiIdentifier: ApiIdentifier)(implicit hc: HeaderCarrier): Future[SubscriptionUpdateResult] = {
    http.POST[ApiIdentifier, Either[UpstreamErrorResponse, Unit]](s"$serviceBaseUrl/application/${applicationId.value}/subscription", apiIdentifier)
      .map {
        case Left(errorResponse) => throw errorResponse
        case Right(_)            => SubscriptionUpdateSuccessResult
      }
  }

  @deprecated("remove after clients are no longer using the old endpoint")
  def addCollaborator(applicationId: ApplicationId, addCollaboratorRequest: AddCollaboratorToTpaRequest)(implicit hc: HeaderCarrier): Future[AddCollaboratorResult] = {
    http.POST[AddCollaboratorToTpaRequest, Either[UpstreamErrorResponse, AddCollaboratorToTpaResponse]](
      s"$serviceBaseUrl/application/${applicationId.value}/collaborator",
      addCollaboratorRequest
    )
      .map {
        case Right(response)                                 => AddCollaboratorSuccessResult(response.registeredUser)
        case Left(UpstreamErrorResponse(_, NOT_FOUND, _, _)) => throw new ApplicationNotFound
        case Left(UpstreamErrorResponse(_, CONFLICT, _, _))  => CollaboratorAlreadyExistsFailureResult
        case Left(errorResponse)                             => throw errorResponse
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
        case Upstream5xxResponse(_, _, _, _) => Set.empty // TODO - really mask this ?
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

@deprecated("remove after clients are no longer using the old endpoint")
sealed trait SubscriptionUpdateResult
case object SubscriptionUpdateSuccessResult extends SubscriptionUpdateResult
case object SubscriptionUpdateFailureResult extends SubscriptionUpdateResult

sealed trait AddCollaboratorResult
case class AddCollaboratorSuccessResult(userRegistered: Boolean) extends AddCollaboratorResult
case object CollaboratorAlreadyExistsFailureResult               extends AddCollaboratorResult
