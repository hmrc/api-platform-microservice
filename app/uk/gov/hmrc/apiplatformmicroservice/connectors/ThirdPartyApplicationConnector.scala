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

package uk.gov.hmrc.apiplatformmicroservice.connectors

import java.net.URLEncoder.encode

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.apiplatformmicroservice.connectors.ThirdPartyApplicationConnector.JsonFormatters.formatApplicationResponse
import uk.gov.hmrc.apiplatformmicroservice.connectors.ThirdPartyApplicationConnector.{ThirdPartyApplicationConnectorConfig, ApplicationResponse}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

abstract class ThirdPartyApplicationConnector(implicit val ec: ExecutionContext) {
  protected val httpClient: HttpClient
  protected val proxiedHttpClient: ProxiedHttpClient
  val serviceBaseUrl: String
  val useProxy: Boolean
  val bearerToken: String
  val apiKey: String

  def http: HttpClient = if (useProxy) proxiedHttpClient.withHeaders(bearerToken, apiKey) else httpClient

  def fetchApplicationsByEmail(email: String)(implicit hc: HeaderCarrier): Future[Seq[String]] = {
    http.GET[Seq[ApplicationResponse]](s"$serviceBaseUrl/developer/applications", Seq("emailAddress" -> email)).map(_.map(_.id))
  }

  def removeCollaborator(applicationId: String, emailAddress: String)(implicit hc: HeaderCarrier): Future[Int] = {
    http.DELETE[HttpResponse](s"$serviceBaseUrl/application/$applicationId/collaborator/${urlEncode(emailAddress)}?adminsToEmail=").map(_.status)
  }

  private def urlEncode(str: String, encoding: String = "UTF-8"): String = {
    encode(str, encoding)
  }
}

object ThirdPartyApplicationConnector {
  private[connectors] case class ApplicationResponse(id: String)
  case class ThirdPartyApplicationConnectorConfig(
    applicationSandboxBaseUrl: String, applicationSandboxUseProxy: Boolean, applicationSandboxBearerToken: String, applicationSandboxApiKey: String,
    applicationProductionBaseUrl: String, applicationProductionUseProxy: Boolean, applicationProductionBearerToken: String, applicationProductionApiKey: String
  )

  object JsonFormatters {
    implicit val formatApplicationResponse: Format[ApplicationResponse] = Json.format[ApplicationResponse]
  }
}

@Singleton
class SandboxThirdPartyApplicationConnector @Inject()(val config: ThirdPartyApplicationConnectorConfig,
                                                      override val httpClient: HttpClient,
                                                      override val proxiedHttpClient: ProxiedHttpClient)(implicit override val ec: ExecutionContext)
  extends ThirdPartyApplicationConnector {

  val serviceBaseUrl = config.applicationSandboxBaseUrl
  val useProxy = config.applicationSandboxUseProxy
  val bearerToken = config.applicationSandboxBearerToken
  val apiKey = config.applicationSandboxApiKey
}

@Singleton
class ProductionThirdPartyApplicationConnector @Inject()(val config: ThirdPartyApplicationConnectorConfig,
                                                         override val httpClient: HttpClient,
                                                         override val proxiedHttpClient: ProxiedHttpClient)(implicit override val ec: ExecutionContext)
  extends ThirdPartyApplicationConnector {

  val serviceBaseUrl = config.applicationProductionBaseUrl
  val useProxy = config.applicationProductionUseProxy
  val bearerToken = config.applicationProductionBearerToken
  val apiKey = config.applicationProductionApiKey
}
