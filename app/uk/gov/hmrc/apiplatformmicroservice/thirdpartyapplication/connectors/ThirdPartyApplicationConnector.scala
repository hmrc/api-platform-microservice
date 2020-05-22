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

import javax.inject.{Inject, Named, Singleton}
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.ThirdPartyApplicationConnector.JsonFormatters.formatApplicationResponse
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.ThirdPartyApplicationConnector._
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

private[thirdpartyapplication] abstract class ThirdPartyApplicationConnector(implicit val ec: ExecutionContext) {
  protected val httpClient: HttpClient
  protected val proxiedHttpClient: ProxiedHttpClient
  protected val config: ThirdPartyApplicationConnectorConfig
  lazy val serviceBaseUrl: String = config.applicationBaseUrl
  lazy val useProxy: Boolean = config.applicationUseProxy
  lazy val bearerToken: String = config.applicationBearerToken
  lazy val apiKey: String = config.applicationApiKey

  def http: HttpClient = if (useProxy) proxiedHttpClient.withHeaders(bearerToken, apiKey) else httpClient

  def fetchApplicationsByEmail(email: String)(implicit hc: HeaderCarrier): Future[Seq[String]] = {
    http.GET[Seq[ApplicationResponse]](s"$serviceBaseUrl/application", Seq("emailAddress" -> email)).map(_.map(_.id.toString))
  }
}

private[thirdpartyapplication] object ThirdPartyApplicationConnector {
  private[connectors] case class ApplicationResponse(id: String)

  object JsonFormatters {
    implicit val formatApplicationResponse: Format[ApplicationResponse] = Json.format[ApplicationResponse]
  }
}

private[thirdpartyapplication] case class ThirdPartyApplicationConnectorConfig(
             applicationBaseUrl: String, applicationUseProxy: Boolean,
             applicationBearerToken: String, applicationApiKey: String
             )

@Singleton
private[thirdpartyapplication] class SubordinateThirdPartyApplicationConnector @Inject()(
                                                      @Named("tpacc-subordinate") override val config: ThirdPartyApplicationConnectorConfig,
                                                      override val httpClient: HttpClient,
                                                      override val proxiedHttpClient: ProxiedHttpClient)
                                                                                        (implicit override val ec: ExecutionContext)
                                                      extends ThirdPartyApplicationConnector

@Singleton
private[thirdpartyapplication] class PrincipalThirdPartyApplicationConnector @Inject()(
                                                         @Named("tpacc-principal") override val config: ThirdPartyApplicationConnectorConfig,
                                                         override val httpClient: HttpClient,
                                                         override val proxiedHttpClient: ProxiedHttpClient)
                                                                                      (implicit override val ec: ExecutionContext)
                                                         extends ThirdPartyApplicationConnector