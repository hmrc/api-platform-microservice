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

package uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.connectors

import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HttpClient, _}

import uk.gov.hmrc.apiplatformmicroservice.common.{EnvironmentAware, ProxiedHttpClient}
import uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.connectors.domain.BoxResponse
import uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.domain._
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.services.ApplicationJsonFormatters

private[pushpullnotifications] object AbstractPushPullNotificationsConnector {

  private[connectors] object JsonFormatters extends ApplicationJsonFormatters {
    import play.api.libs.json._

    implicit val readsBoxId: Format[BoxId]                  = Json.valueFormat[BoxId]
    implicit val readsBoxCreator: Reads[BoxCreator]         = Json.reads[BoxCreator]
    implicit val readsBoxSubscriber: OFormat[BoxSubscriber] = Json.format[BoxSubscriber]
    implicit val readsBox: Reads[BoxResponse]               = Json.reads[BoxResponse]
  }

  case class Config(
      applicationBaseUrl: String,
      applicationUseProxy: Boolean,
      applicationBearerToken: String,
      applicationApiKey: String
    )
}

trait PushPullNotificationsConnector {
  def fetchAllBoxes()(implicit hc: HeaderCarrier): Future[List[BoxResponse]]
}

abstract private[pushpullnotifications] class AbstractPushPullNotificationsConnector(implicit val ec: ExecutionContext) extends PushPullNotificationsConnector {
  import AbstractPushPullNotificationsConnector.JsonFormatters._

  protected val httpClient: HttpClient
  protected val proxiedHttpClient: ProxiedHttpClient
  protected val config: AbstractPushPullNotificationsConnector.Config
  lazy val serviceBaseUrl: String = config.applicationBaseUrl

  lazy val useProxy: Boolean   = config.applicationUseProxy
  lazy val bearerToken: String = config.applicationBearerToken
  lazy val apiKey: String      = config.applicationApiKey

  def http: HttpClient = if (useProxy) proxiedHttpClient.withHeaders(bearerToken, apiKey) else httpClient

  def fetchAllBoxes()(implicit hc: HeaderCarrier): Future[List[BoxResponse]] = {
    val url = s"$serviceBaseUrl/box"
    http.GET[List[BoxResponse]](url)
  }
}

@Singleton
@Named("subordinate")
class SubordinatePushPullNotificationsConnector @Inject() (
    @Named("subordinate") override val config: AbstractPushPullNotificationsConnector.Config,
    override val httpClient: HttpClient,
    override val proxiedHttpClient: ProxiedHttpClient
  )(implicit override val ec: ExecutionContext
  ) extends AbstractPushPullNotificationsConnector {}

@Singleton
@Named("principal")
class PrincipalPushPullNotificationsConnector @Inject() (
    @Named("principal") override val config: AbstractPushPullNotificationsConnector.Config,
    override val httpClient: HttpClient,
    override val proxiedHttpClient: ProxiedHttpClient
  )(implicit override val ec: ExecutionContext
  ) extends AbstractPushPullNotificationsConnector {}

@Singleton
class EnvironmentAwarePushPullNotificationsConnector @Inject() (
    @Named("subordinate") val subordinate: PushPullNotificationsConnector,
    @Named("principal") val principal: PushPullNotificationsConnector
  ) extends EnvironmentAware[PushPullNotificationsConnector]
