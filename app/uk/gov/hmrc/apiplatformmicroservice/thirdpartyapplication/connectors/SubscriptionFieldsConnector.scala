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
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, UpstreamErrorResponse}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.{Environment, _}
import uk.gov.hmrc.apiplatform.modules.subscriptions.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.common.{EnvironmentAware, ProxiedHttpClient}

private[thirdpartyapplication] trait SubscriptionFieldsConnector {

  import SubscriptionFieldsConnectorDomain._

  def bulkFetchFieldDefinitions(implicit hc: HeaderCarrier): Future[ApiFieldMap[FieldDefinition]]

  def bulkFetchFieldValues(clientId: ClientId)(implicit hc: HeaderCarrier): Future[ApiFieldMap[FieldValue]]

  def saveFieldValues(clientId: ClientId, apiIdentifier: ApiIdentifier, values: Map[FieldName, FieldValue])(implicit hc: HeaderCarrier): Future[Either[FieldErrors, Unit]]
}

abstract private[thirdpartyapplication] class AbstractSubscriptionFieldsConnector(implicit ec: ExecutionContext) extends SubscriptionFieldsConnector {

  val environment: Environment
  val serviceBaseUrl: String

  import SubscriptionFieldsConnectorDomain._
  import SubscriptionFieldsConnectorDomain.JsonFormatters._

  protected def http: HttpClient

  def bulkFetchFieldDefinitions(implicit hc: HeaderCarrier): Future[ApiFieldMap[FieldDefinition]] = {
    http.GET[BulkApiFieldDefinitionsResponse](urlBulkSubscriptionFieldDefinitions)
      .map(r => asMapOfMapsOfFieldDefns(r.apis))
  }

  def bulkFetchFieldValues(clientId: ClientId)(implicit hc: HeaderCarrier): Future[ApiFieldMap[FieldValue]] = {

    val url = urlBulkSubscriptionFieldValues(clientId)
    http.GET[Option[BulkSubscriptionFieldsResponse]](url)
      .map(_.fold(Map.empty[ApiContext, Map[ApiVersionNbr, Map[FieldName, FieldValue]]])(r => asMapOfMaps(r.subscriptions)))
  }

  def saveFieldValues(clientId: ClientId, apiIdentifier: ApiIdentifier, fields: Map[FieldName, FieldValue])(implicit hc: HeaderCarrier): Future[Either[FieldErrors, Unit]] = {
    lazy val url = urlSubscriptionFieldValues(clientId, apiIdentifier)

    if (fields.isEmpty) {
      successful(Right(()))
    } else {
      http.PUT[SubscriptionFieldsPutRequest, HttpResponse](url, SubscriptionFieldsPutRequest(clientId, apiIdentifier.context, apiIdentifier.versionNbr, fields)).map { response =>
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
    s"$serviceBaseUrl/definition"

  private def urlBulkSubscriptionFieldValues(clientId: ClientId) =
    s"$serviceBaseUrl/field/application/$clientId"

  private def urlSubscriptionFieldValues(clientId: ClientId, apiIdentifier: ApiIdentifier) =
    s"$serviceBaseUrl/field/application/$clientId/context/${apiIdentifier.context.value}/version/${apiIdentifier.versionNbr.value}"
}

object SubordinateSubscriptionFieldsConnector {
  case class Config(serviceBaseUrl: String, useProxy: Boolean, bearerToken: String, apiKey: String)
}

@Singleton
class SubordinateSubscriptionFieldsConnector @Inject() (
    val config: SubordinateSubscriptionFieldsConnector.Config,
    val httpClient: HttpClient,
    val proxiedHttpClient: ProxiedHttpClient
  )(implicit val ec: ExecutionContext
  ) extends AbstractSubscriptionFieldsConnector {

  val environment: Environment = Environment.SANDBOX
  val serviceBaseUrl: String   = config.serviceBaseUrl
  val useProxy: Boolean        = config.useProxy
  val bearerToken: String      = config.bearerToken
  val apiKey: String           = config.apiKey

  protected def http: HttpClient = if (useProxy) proxiedHttpClient.withHeaders(bearerToken, apiKey) else httpClient
}

object PrincipalSubscriptionFieldsConnector {
  case class Config(serviceBaseUrl: String)
}

@Singleton
class PrincipalSubscriptionFieldsConnector @Inject() (
    val config: PrincipalSubscriptionFieldsConnector.Config,
    val http: HttpClient
  )(implicit val ec: ExecutionContext
  ) extends AbstractSubscriptionFieldsConnector {

  val environment: Environment = Environment.PRODUCTION
  val serviceBaseUrl: String   = config.serviceBaseUrl
}

@Singleton
class EnvironmentAwareSubscriptionFieldsConnector @Inject() (
    @Named("subordinate") val subordinate: SubscriptionFieldsConnector,
    @Named("principal") val principal: SubscriptionFieldsConnector
  ) extends EnvironmentAware[SubscriptionFieldsConnector]
