/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.connectors

import javax.inject.{Inject, Named, Singleton}
import play.api.http.Status._
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.{ApiContext, ApiIdentifier, ApiVersion}
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.common.{EnvironmentAware, ProxiedHttpClient}
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.apiplatformmicroservice.common.ApplicationLogger

import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json.Json
import uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.domain.Box

private[pushpullnotifications] object AbstractPushPullNotificationsConnector {

  private[connectors] object JsonFormatters {
    import play.api.libs.json._
    
    implicit val readsBox = Json.reads[Box]
  }

  case class Config(
      applicationBaseUrl: String,
      applicationUseProxy: Boolean,
      applicationBearerToken: String,
      applicationApiKey: String)
}

trait PushPullNotificationsConnector {
  def fetchAllBoxes()(implicit hc: HeaderCarrier): Future[List[Box]]
}

private[pushpullnotifications] abstract class AbstractPushPullNotificationsConnector(implicit val ec: ExecutionContext) extends PushPullNotificationsConnector {
  import uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.connectors.AbstractPushPullNotificationsConnector._
  import AbstractPushPullNotificationsConnector.JsonFormatters._

  protected val httpClient: HttpClient
  protected val proxiedHttpClient: ProxiedHttpClient
  protected val config: AbstractPushPullNotificationsConnector.Config
  lazy val serviceBaseUrl: String = config.applicationBaseUrl

  // TODO Tidy this like Subs Fields to remove redundant "fixed" config for Principal connector
  lazy val useProxy: Boolean = config.applicationUseProxy
  lazy val bearerToken: String = config.applicationBearerToken
  lazy val apiKey: String = config.applicationApiKey

  def http: HttpClient = if (useProxy) proxiedHttpClient.withHeaders(bearerToken, apiKey) else httpClient

  def fetchAllBoxes()(implicit hc: HeaderCarrier): Future[List[Box]] = {
    // TODO: Fix URL
    val url = s"$serviceBaseUrl/application/"
    http.GET[List[Box]](url)
  }
}

@Singleton
@Named("subordinate")
class SubordinatePushPullNotificationsConnector @Inject() (
    @Named("subordinate") override val config: AbstractPushPullNotificationsConnector.Config,
    override val httpClient: HttpClient,
    override val proxiedHttpClient: ProxiedHttpClient
  )(implicit override val ec: ExecutionContext)
    extends AbstractPushPullNotificationsConnector {
}

@Singleton
@Named("principal")
class PrincipalPushPullNotificationsConnector @Inject() (
    @Named("principal") override val config: AbstractPushPullNotificationsConnector.Config,
    override val httpClient: HttpClient,
    override val proxiedHttpClient: ProxiedHttpClient
  )(implicit override val ec: ExecutionContext)
    extends AbstractPushPullNotificationsConnector {

}

@Singleton
class EnvironmentAwarePushPullNotificationsConnector @Inject() (
    @Named("subordinate") val subordinate: PushPullNotificationsConnector,
    @Named("principal") val principal: PushPullNotificationsConnector)
    extends EnvironmentAware[PushPullNotificationsConnector]
