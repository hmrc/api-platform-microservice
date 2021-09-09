/*
 * Copyright 2021 HM Revenue & Customs
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
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.connectors.PrincipalApiDefinitionConnector.Config
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ResourceId
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.play.http.ws.WSGet
import uk.gov.hmrc.apiplatformmicroservice.common.ApplicationLogger

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PrincipalApiDefinitionConnector @Inject() (
  val http: HttpClient with WSGet,
  val config: Config
)(
  implicit val ec: ExecutionContext
) extends ApiDefinitionConnector with ApplicationLogger {
  val serviceBaseUrl: String = config.baseUrl

  override def fetchApiDocumentationResource(
      resourceId: ResourceId
    )(implicit hc: HeaderCarrier
    ): Future[Option[WSResponse]] = {
    val url = documentationUrl(serviceBaseUrl, resourceId)

    logger.info(
      s"${this.getClass.getSimpleName} - P - fetchApiDocumentationResource. Url: $url"
    )

    http
      .buildRequest(url, Seq.empty)
      .stream()
      .map(Some(_))
  }
}

object PrincipalApiDefinitionConnector {
  case class Config(baseUrl: String)
}
