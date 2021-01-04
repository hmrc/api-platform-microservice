/*
 * Copyright 2021 HM Revenue & Customs
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
import play.api.http.Status._
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.{ApiContext, ApiIdentifier, ApiVersion}
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.common.{EnvironmentAware, ProxiedHttpClient}
import uk.gov.hmrc.apiplatformmicroservice.common.domain.services.CommonJsonFormatters
import domain.{AddCollaboratorToTpaRequest, AddCollaboratorToTpaResponse}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.Application
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.services.ApplicationJsonFormatters._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.UpstreamErrorResponse.WithStatusCode
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

private[thirdpartyapplication] object AbstractThirdPartyApplicationConnector extends CommonJsonFormatters {

  class ApplicationNotFound extends RuntimeException
  class TeamMemberAlreadyExists extends RuntimeException("This user is already a teamMember on this application.")

  private[connectors] case class ApplicationResponse(id: ApplicationId)

  // N.B. This is a small subsection of the model that is normally returned
  private[connectors] case class InnerVersion(version: ApiVersion)
  private[connectors] case class SubscriptionVersion(version: InnerVersion, subscribed: Boolean)
  private[connectors] case class Subscription(context: ApiContext, versions: Seq[SubscriptionVersion])

  private[connectors] object JsonFormatters extends CommonJsonFormatters {
    import play.api.libs.json._

    implicit val readsApiIdentifier = Json.reads[ApiIdentifier]
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

trait ThirdPartyApplicationConnector {
  def fetchApplication(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[Application]]

  def fetchApplicationsByEmail(email: String)(implicit hc: HeaderCarrier): Future[Seq[ApplicationId]]

  def fetchSubscriptionsByEmail(email: String)(implicit hc: HeaderCarrier): Future[Seq[ApiIdentifier]]

  def fetchSubscriptionsById(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Set[ApiIdentifier]]

  def subscribeToApi(applicationId: ApplicationId, apiIdentifier: ApiIdentifier)(implicit hc: HeaderCarrier): Future[SubscriptionUpdateResult]

  def addCollaborator(applicationId: ApplicationId, addCollaboratorRequest: AddCollaboratorToTpaRequest)(implicit hc: HeaderCarrier): Future[AddCollaboratorResult]
}

private[thirdpartyapplication] abstract class AbstractThirdPartyApplicationConnector(implicit val ec: ExecutionContext) extends ThirdPartyApplicationConnector {
  import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.AbstractThirdPartyApplicationConnector._
  import AbstractThirdPartyApplicationConnector.JsonFormatters._

  protected val httpClient: HttpClient
  protected val proxiedHttpClient: ProxiedHttpClient
  protected val config: AbstractThirdPartyApplicationConnector.Config
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

  def fetchSubscriptionsById(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Set[ApiIdentifier]] = {
    http.GET[Set[ApiIdentifier]](s"$serviceBaseUrl/application/${applicationId.value}/subscription")
      .recover {
        case WithStatusCode(NOT_FOUND, _) => throw new ApplicationNotFound
      }
  }

  def subscribeToApi(applicationId: ApplicationId, apiIdentifier: ApiIdentifier)(implicit hc: HeaderCarrier): Future[SubscriptionUpdateResult] = {
    http.POST[ApiIdentifier, Either[UpstreamErrorResponse,Unit]](s"$serviceBaseUrl/application/${applicationId.value}/subscription", apiIdentifier)
    .map( _ match {
      case Left(errorResponse) => throw errorResponse
      case Right(_) =>  SubscriptionUpdateSuccessResult
    })
  }

  def addCollaborator(applicationId: ApplicationId, addCollaboratorRequest: AddCollaboratorToTpaRequest)
                     (implicit hc: HeaderCarrier): Future[AddCollaboratorResult] = {
    http.POST[AddCollaboratorToTpaRequest, Either[UpstreamErrorResponse, AddCollaboratorToTpaResponse]](s"$serviceBaseUrl/application/${applicationId.value}/collaborator", addCollaboratorRequest)
    .map(_ match {
      case Right(response) => AddCollaboratorSuccessResult(response.registeredUser)
      case Left(UpstreamErrorResponse(_,NOT_FOUND, _, _)) => throw new ApplicationNotFound
      case Left(UpstreamErrorResponse(_,CONFLICT, _, _)) => CollaboratorAlreadyExistsFailureResult
      case Left(errorResponse) => throw errorResponse
    })
  }
}

@Singleton
@Named("subordinate")
class SubordinateThirdPartyApplicationConnector @Inject() (
    @Named("subordinate") override val config: AbstractThirdPartyApplicationConnector.Config,
    override val httpClient: HttpClient,
    override val proxiedHttpClient: ProxiedHttpClient
  )(implicit override val ec: ExecutionContext)
    extends AbstractThirdPartyApplicationConnector {

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
  )(implicit override val ec: ExecutionContext)
    extends AbstractThirdPartyApplicationConnector

@Singleton
class EnvironmentAwareThirdPartyApplicationConnector @Inject() (
    @Named("subordinate") val subordinate: ThirdPartyApplicationConnector,
    @Named("principal") val principal: ThirdPartyApplicationConnector)
    extends EnvironmentAware[ThirdPartyApplicationConnector]

sealed trait SubscriptionUpdateResult
case object SubscriptionUpdateSuccessResult extends SubscriptionUpdateResult
case object SubscriptionUpdateFailureResult extends SubscriptionUpdateResult

sealed trait AddCollaboratorResult
case class AddCollaboratorSuccessResult(userRegistered: Boolean) extends AddCollaboratorResult
case object CollaboratorAlreadyExistsFailureResult extends AddCollaboratorResult
