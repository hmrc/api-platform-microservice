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

import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import uk.gov.hmrc.apiplatformmicroservice.apidefinition.connectors.PrincipalApiDefinitionConnector.Config
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ResourceId
import uk.gov.hmrc.apiplatformmicroservice.common.ApplicationLogger

@Singleton
class PrincipalApiDefinitionConnector @Inject() (
    val http: HttpClientV2,
    val config: Config
  )(implicit val ec: ExecutionContext
  ) extends ApiDefinitionConnector with ApplicationLogger {
  val serviceBaseUrl: String = config.baseUrl

  val configureEbridgeIfRequired: RequestBuilder => RequestBuilder = identity

  override def fetchApiDocumentationResource(
      resourceId: ResourceId
    )(implicit hc: HeaderCarrier
    ): Future[Option[HttpResponse]] = {
    val theUrl = url"${documentationUrl(resourceId)}"

    logger.info(
      s"${this.getClass.getSimpleName} - P - fetchApiDocumentationResource. Url: $theUrl"
    )

    http.get(theUrl).stream[HttpResponse].map(Some(_))
  }
}

object PrincipalApiDefinitionConnector {
  case class Config(baseUrl: String)
}
