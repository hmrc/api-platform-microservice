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

import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}

import play.api.http.Status._
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.{Environment, _}
import uk.gov.hmrc.apiplatform.modules.applications.subscriptions.domain.models._
import uk.gov.hmrc.apiplatform.modules.subscriptions.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.common.EnvironmentAware
import uk.gov.hmrc.apiplatformmicroservice.common.utils.EbridgeConfigurator
import uk.gov.hmrc.apiplatform.modules.applications.subscriptions.interface.models.BulkSubscriptionFieldsResponse

private[thirdpartyapplication] trait SubscriptionFieldsConnector {

  import SubscriptionFieldsConnectorDomain._

  def bulkFetchFieldDefinitions(implicit hc: HeaderCarrier): Future[ApiFieldMap[FieldDefinition]]

  def bulkFetchFieldValues(clientId: ClientId)(implicit hc: HeaderCarrier): Future[ApiFieldMap[FieldValue]]

  def saveFieldValues(clientId: ClientId, apiIdentifier: ApiIdentifier, values: Map[FieldName, FieldValue])(implicit hc: HeaderCarrier): Future[Either[FieldErrors, Unit]]
}

abstract private[thirdpartyapplication] class AbstractSubscriptionFieldsConnector(implicit ec: ExecutionContext) extends SubscriptionFieldsConnector {

  def serviceBaseUrl: String
  def http: HttpClientV2

  def configureEbridgeIfRequired: RequestBuilder => RequestBuilder

  import SubscriptionFieldsConnectorDomain._
  import SubscriptionFieldsConnectorDomain.JsonFormatters._

  def bulkFetchFieldDefinitions(implicit hc: HeaderCarrier): Future[ApiFieldMap[FieldDefinition]] = {
    configureEbridgeIfRequired(
      http.get(urlBulkSubscriptionFieldDefinitions)
    )
      .execute[BulkApiFieldDefinitionsResponse]
      .map(r => asMapOfMapsOfFieldDefns(r.apis))
  }

  def bulkFetchFieldValues(clientId: ClientId)(implicit hc: HeaderCarrier): Future[ApiFieldMap[FieldValue]] = {
    configureEbridgeIfRequired(
      http.get(urlBulkSubscriptionFieldValues(clientId))
    )
      .execute[Option[BulkSubscriptionFieldsResponse]]
      .map(_.fold(Map.empty[ApiContext, Map[ApiVersionNbr, Map[FieldName, FieldValue]]])(BulkSubscriptionFieldsResponse.toApiFieldMap))
  }

  def saveFieldValues(clientId: ClientId, apiIdentifier: ApiIdentifier, fields: Map[FieldName, FieldValue])(implicit hc: HeaderCarrier): Future[Either[FieldErrors, Unit]] = {
    if (fields.isEmpty) {
      successful(Right(()))
    } else {
      configureEbridgeIfRequired(
        http.put(urlSubscriptionFieldValues(clientId, apiIdentifier))
          .withBody(Json.toJson(SubscriptionFieldsPutRequest(clientId, apiIdentifier.context, apiIdentifier.versionNbr, fields)))
      )
        .execute[HttpResponse]
        .map { response =>
          response.status match {
            case BAD_REQUEST  =>
              Json.parse(response.body).validate[Map[FieldName, String]] match {
                case s: JsSuccess[Map[FieldName, String]] => Left(s.get)
                case _                                    => Left(Map.empty)
              }
            case OK | CREATED => Right(())
            case statusCode   => throw UpstreamErrorResponse("Failed to put subscription fields", statusCode)
          }
        }
    }
  }

  private lazy val urlBulkSubscriptionFieldDefinitions =
    url"$serviceBaseUrl/definition"

  def urlBulkSubscriptionFieldValues(clientId: ClientId) =
    url"$serviceBaseUrl/field/application/${clientId}"

  def urlSubscriptionFieldValues(clientId: ClientId, apiIdentifier: ApiIdentifier) =
    url"$serviceBaseUrl/field/application/${clientId}/context/${apiIdentifier.context}/version/${apiIdentifier.versionNbr}"
}

object SubordinateSubscriptionFieldsConnector {
  case class Config(serviceBaseUrl: String, useProxy: Boolean, bearerToken: String, apiKey: String)
}

@Singleton
class SubordinateSubscriptionFieldsConnector @Inject() (
    val config: SubordinateSubscriptionFieldsConnector.Config,
    val http: HttpClientV2
  )(implicit val ec: ExecutionContext
  ) extends AbstractSubscriptionFieldsConnector {

  val environment: Environment = Environment.SANDBOX
  val serviceBaseUrl: String   = config.serviceBaseUrl
  val useProxy: Boolean        = config.useProxy
  val bearerToken: String      = config.bearerToken
  val apiKey: String           = config.apiKey

  lazy val configureEbridgeIfRequired: RequestBuilder => RequestBuilder =
    EbridgeConfigurator.configure(useProxy, bearerToken, apiKey)

}

object PrincipalSubscriptionFieldsConnector {
  case class Config(serviceBaseUrl: String)
}

@Singleton
class PrincipalSubscriptionFieldsConnector @Inject() (
    val config: PrincipalSubscriptionFieldsConnector.Config,
    val http: HttpClientV2
  )(implicit val ec: ExecutionContext
  ) extends AbstractSubscriptionFieldsConnector {

  val configureEbridgeIfRequired: RequestBuilder => RequestBuilder = identity

  val environment: Environment = Environment.PRODUCTION
  val serviceBaseUrl: String   = config.serviceBaseUrl
}

@Singleton
class EnvironmentAwareSubscriptionFieldsConnector @Inject() (
    @Named("subordinate") val subordinate: SubscriptionFieldsConnector,
    @Named("principal") val principal: SubscriptionFieldsConnector
  ) extends EnvironmentAware[SubscriptionFieldsConnector]
