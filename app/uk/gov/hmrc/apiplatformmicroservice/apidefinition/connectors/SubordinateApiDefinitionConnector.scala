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

package uk.gov.hmrc.apiplatformmicroservice.apidefinition.connectors

import akka.actor.ActorSystem
import akka.pattern.FutureTimeoutSupport
import akka.stream.Materializer
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.connectors.SubordinateApiDefinitionConnector._
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.{APIDefinition, ResourceId}
import uk.gov.hmrc.apiplatformmicroservice.common.ProxiedHttpClient
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.http.ws.WSGet

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubordinateApiDefinitionConnector @Inject() (
    val config: Config,
    val httpClient: HttpClient with WSGet,
    val proxiedHttpClient: ProxiedHttpClient,
    val actorSystem: ActorSystem,
    val futureTimeout: FutureTimeoutSupport
  )(implicit val ec: ExecutionContext,
    val mat: Materializer)
    extends ApiDefinitionConnector
    with Retries
    with WSResponseRetries {

  val serviceBaseUrl: String = config.serviceBaseUrl
  val retryCount: Int = config.retryCount
  val retryDelayMilliseconds: Int = config.retryDelayMilliseconds

  import config._

  override def http: HttpClient with WSGet =
    if (useProxy) {
      proxiedHttpClient.withHeaders(bearerToken, apiKey)
    } else {
      httpClient
    }

  override def fetchAllApiDefinitions(implicit hc: HeaderCarrier): Future[Seq[APIDefinition]] = {
    retry {
      super.fetchAllApiDefinitions
    }
  }

  override def fetchApiDefinition(serviceName: String)(implicit hc: HeaderCarrier): Future[Option[APIDefinition]] = {
    retry {
      super.fetchApiDefinition(serviceName)
    }
  }

  override def fetchApiDocumentationResource(resourceId: ResourceId)(implicit hc: HeaderCarrier): Future[Option[WSResponse]] = {
    val url = documentationUrl(serviceBaseUrl, resourceId)

    Logger.info(s"${this.getClass.getSimpleName} - S - fetchApiDocumentationResource. Url: $url")

    if (useProxy) {
      retryWSResponse {
        proxiedHttpClient
          .withHeaders(bearerToken, apiKey)
          .buildRequest(url)
          .stream()
          .map(Some(_))
      }
    } else {
      Future.successful(None)
    }
  }
}

object SubordinateApiDefinitionConnector {

  case class Config(
      serviceBaseUrl: String,
      useProxy: Boolean,
      bearerToken: String,
      apiKey: String,
      retryCount: Int,
      retryDelayMilliseconds: Int)
}
