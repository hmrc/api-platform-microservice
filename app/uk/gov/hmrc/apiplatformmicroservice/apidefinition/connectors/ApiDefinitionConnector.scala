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

import play.api.Logger
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.{APICategoryDetails, APIDefinition, ApiDefinitionJsonFormatters, ResourceId}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.http.ws.WSGet

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import uk.gov.hmrc.http.HttpReads.Implicits._

trait ApiDefinitionConnector extends ApiDefinitionConnectorUtils with ApiDefinitionJsonFormatters {
  def http: HttpClient with WSGet
  def serviceBaseUrl: String
  implicit val ec: ExecutionContext

  def fetchAllApiDefinitions(implicit hc: HeaderCarrier): Future[List[APIDefinition]] = {
    Logger.info(s"${this.getClass.getSimpleName} - fetchAllApiDefinitionsWithoutFiltering")
    http.GET[Option[List[APIDefinition]]](definitionsUrl(serviceBaseUrl), Seq("type" -> "all"))
    .map(_ match {
      case None => List.empty
      case Some(apiDefinitions) => apiDefinitions.sortBy(_.name)
    })
  }

  def fetchApiDefinition(serviceName: String)(implicit hc: HeaderCarrier): Future[Option[APIDefinition]] = {
    Logger.info(s"${this.getClass.getSimpleName} - fetchApiDefinition")
    val r = http.GET[Option[APIDefinition]](definitionUrl(serviceBaseUrl, serviceName))

    r.recover {
      case NonFatal(e)          =>
        Logger.error(s"Failed $e")
        throw e
    }
  }

  def fetchApiCategoryDetails()(implicit hc: HeaderCarrier): Future[Seq[APICategoryDetails]] = {
    Logger.info(s"${this.getClass.getSimpleName} - fetchApiCategoryDetails")

    http.GET[Seq[APICategoryDetails]](categoriesUrl(serviceBaseUrl))
      .recover {
        case NonFatal(e) =>
          Logger.error(s"Failed $e")
          throw e
      }
  }

  def fetchApiDocumentationResource(resourceId: ResourceId)(implicit hc: HeaderCarrier): Future[Option[WSResponse]]
}
