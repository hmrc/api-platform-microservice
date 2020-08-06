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

import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.http.HttpReads.Implicits._

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.apiplatformmicroservice.common.ProxiedHttpClient
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.SubscriptionFieldsService._
import uk.gov.hmrc.http.HeaderCarrier
import akka.pattern.FutureTimeoutSupport
import akka.actor.ActorSystem
import com.google.inject.{Inject, Singleton}

import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.ApiIdentifier
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications._

abstract class AbstractSubscriptionFieldsConnector(implicit ec: ExecutionContext) extends SubscriptionFieldsConnector {
  protected val httpClient: HttpClient
  protected val proxiedHttpClient: ProxiedHttpClient
  val environment: Environment
  val serviceBaseUrl: String

  import SubscriptionFieldsConnectorDomain._

  def http: HttpClient

  def fetchFieldValues(
      clientId: ClientId,
      apiIdentifier: ApiIdentifier
    )(implicit hc: HeaderCarrier
    ): Future[Map[FieldName, FieldValue]] = {

    val url = urlSubscriptionFieldValues(clientId, apiIdentifier)
    http.GET[Option[ApplicationApiFieldValues]](url)
      .map(_.fold(Map.empty[FieldName, FieldValue])(_.fields))
  }

  private def urlEncode(str: String): String = encode(str, "UTF-8")

  private def urlEncode(apiIdentifier: ApiIdentifier): String = s"context/${urlEncode(apiIdentifier.context.value)}/version/${urlEncode(apiIdentifier.version.value)}"

  private def urlSubscriptionFieldValues(clientId: ClientId, apiIdentifier: ApiIdentifier) =
    s"$serviceBaseUrl/field/application/${urlEncode(clientId.value)}/${urlEncode(apiIdentifier)}"
}

object SubordinateSubscriptionFieldsConnector {
  case class Config(serviceBaseUrl: String, useProxy: Boolean, bearerToken: String, apiKey: String)
}

@Singleton
class SubordinateSubscriptionFieldsConnector @Inject() (
    val appConfig: SubordinateSubscriptionFieldsConnector.Config,
    val httpClient: HttpClient,
    val proxiedHttpClient: ProxiedHttpClient,
    val actorSystem: ActorSystem,
    val futureTimeout: FutureTimeoutSupport
  )(implicit val ec: ExecutionContext)
    extends AbstractSubscriptionFieldsConnector {

  val environment: Environment = Environment.SANDBOX
  val serviceBaseUrl: String = appConfig.serviceBaseUrl
  val useProxy: Boolean = appConfig.useProxy
  val bearerToken: String = appConfig.bearerToken
  val apiKey: String = appConfig.apiKey

  def http: HttpClient = if (useProxy) proxiedHttpClient.withHeaders(bearerToken, apiKey) else httpClient
}

object PrincipalSubscriptionFieldsConnector {
  case class Config(serviceBaseUrl: String)
}

@Singleton
class PrincipalSubscriptionFieldsConnector @Inject() (
    val appConfig: PrincipalSubscriptionFieldsConnector.Config,
    val httpClient: HttpClient,
    val proxiedHttpClient: ProxiedHttpClient,
    val actorSystem: ActorSystem,
    val futureTimeout: FutureTimeoutSupport
  )(implicit val ec: ExecutionContext)
    extends AbstractSubscriptionFieldsConnector {

  def http: HttpClient = httpClient

  val environment: Environment = Environment.PRODUCTION
  val serviceBaseUrl: String = appConfig.serviceBaseUrl
}
