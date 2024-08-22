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

package uk.gov.hmrc.apiplatformmicroservice.apidefinition.connectors

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import org.apache.pekko.pattern.FutureTimeoutSupport
import org.apache.pekko.stream.Materializer

import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import uk.gov.hmrc.apiplatformmicroservice.apidefinition.connectors.SubordinateApiDefinitionConnector._
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ResourceId
import uk.gov.hmrc.apiplatformmicroservice.common.ApplicationLogger
import uk.gov.hmrc.apiplatformmicroservice.common.utils.EbridgeConfigurator

@Singleton
class SubordinateApiDefinitionConnector @Inject() (
    val config: Config,
    val http: HttpClientV2,
    val futureTimeout: FutureTimeoutSupport
  )(implicit val ec: ExecutionContext,
    val mat: Materializer
  ) extends ApiDefinitionConnector with ApplicationLogger {

  lazy val configureEbridgeIfRequired: RequestBuilder => RequestBuilder =
    EbridgeConfigurator.configure(config.useProxy, config.bearerToken, config.apiKey)

  val serviceBaseUrl: String = config.serviceBaseUrl

  override def fetchApiDocumentationResource(resourceId: ResourceId)(implicit hc: HeaderCarrier): Future[Option[HttpResponse]] = {
    val theUrl = documentationUrl(resourceId)

    logger.info(s"${this.getClass.getSimpleName} - S - fetchApiDocumentationResource. Url: $theUrl")

    if (config.useProxy) {
      configureEbridgeIfRequired(
        http.get(theUrl)
      )
        .stream[HttpResponse]
        .map(Some(_))
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
      apiKey: String
    )
}
