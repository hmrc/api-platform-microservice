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

import java.net.URLEncoder.encode

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.apiplatformmicroservice.common.ProxiedHttpClient
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.HeaderCarrier
import com.google.inject.{Inject, Singleton}
import com.google.inject.name.Named
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications._
import uk.gov.hmrc.apiplatformmicroservice.common.EnvironmentAwareConnector
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models._

private[thirdpartyapplication] trait SubscriptionFieldsConnector {
  def bulkFetchFieldValues(clientId: ClientId)(implicit hc: HeaderCarrier): Future[Map[ApiContext, Map[ApiVersion, Map[FieldName, FieldValue]]]]
}

private[thirdpartyapplication] abstract class AbstractSubscriptionFieldsConnector(implicit ec: ExecutionContext) extends SubscriptionFieldsConnector {

  val environment: Environment
  val serviceBaseUrl: String

  import SubscriptionFieldsConnectorDomain._
  import SubscriptionFieldsConnectorDomain.JsonFormatters._

  def http: HttpClient

  def bulkFetchFieldValues(
      clientId: ClientId
    )(implicit hc: HeaderCarrier
    ): Future[Map[ApiContext, Map[ApiVersion, Map[FieldName, FieldValue]]]] = {

    val url = urlBulkSubscriptionFieldValues(clientId)
    http.GET[Option[BulkSubscriptionFieldsResponse]](url)
      .map(_.fold(Map.empty[ApiContext, Map[ApiVersion, Map[FieldName, FieldValue]]])(r => asMapOfMaps(r.subscriptions)))
  }

  private def urlEncode(str: String): String = encode(str, "UTF-8")

  private def urlBulkSubscriptionFieldValues(clientId: ClientId) =
    s"$serviceBaseUrl/field/application/${urlEncode(clientId.value)}"
}

object SubordinateSubscriptionFieldsConnector {
  case class Config(serviceBaseUrl: String, useProxy: Boolean, bearerToken: String, apiKey: String)
}

@Singleton
class SubordinateSubscriptionFieldsConnector @Inject() (
    val config: SubordinateSubscriptionFieldsConnector.Config,
    val httpClient: HttpClient,
    val proxiedHttpClient: ProxiedHttpClient
  )(implicit val ec: ExecutionContext)
    extends AbstractSubscriptionFieldsConnector {

  val environment: Environment = Environment.SANDBOX
  val serviceBaseUrl: String = config.serviceBaseUrl
  val useProxy: Boolean = config.useProxy
  val bearerToken: String = config.bearerToken
  val apiKey: String = config.apiKey

  def http: HttpClient = if (useProxy) proxiedHttpClient.withHeaders(bearerToken, apiKey) else httpClient
}

object PrincipalSubscriptionFieldsConnector {
  case class Config(serviceBaseUrl: String)
}

@Singleton
class PrincipalSubscriptionFieldsConnector @Inject() (
    val config: PrincipalSubscriptionFieldsConnector.Config,
    val http: HttpClient
  )(implicit val ec: ExecutionContext)
    extends AbstractSubscriptionFieldsConnector {

  val environment: Environment = Environment.PRODUCTION
  val serviceBaseUrl: String = config.serviceBaseUrl
}

@Singleton
class EnvironmentAwareSubscriptionFieldsConnector @Inject() (
    @Named("subordinate") val subordinate: SubscriptionFieldsConnector,
    @Named("principal") val principal: SubscriptionFieldsConnector)
    extends EnvironmentAwareConnector[SubscriptionFieldsConnector]
