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

import scala.concurrent.{ExecutionContext, Future}

import play.api.libs.json.{JsObject, JsValue}
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import uk.gov.hmrc.play.http.ws.WSGet

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.{ApiCategoryDetails, ApiDefinition, ApiDefinitionJsonFormatters, ResourceId}
import uk.gov.hmrc.apiplatformmicroservice.common.ApplicationLogger
import uk.gov.hmrc.apiplatformmicroservice.common.connectors.ConnectorRecovery

trait ApiDefinitionConnector extends ApiDefinitionConnectorUtils with ApiDefinitionJsonFormatters
    with ApplicationLogger with ConnectorRecovery {

  def http: HttpClient with WSGet
  def serviceBaseUrl: String
  implicit val ec: ExecutionContext

  def fetchAllApiDefinitions(implicit hc: HeaderCarrier): Future[List[ApiDefinition]] = {
    logger.info(s"${this.getClass.getSimpleName} - fetchAllApiDefinitionsWithoutFiltering")
    http.GET[Option[List[ApiDefinition]]](definitionsUrl, Seq("type" -> "all"))
      .map(_ match {
        case None                 => List.empty
        case Some(apiDefinitions) => apiDefinitions.sortBy(_.name)
      })
  }

  def fetchApiDefinition(serviceName: String)(implicit hc: HeaderCarrier): Future[Option[ApiDefinition]] = {
    logger.info(s"${this.getClass.getSimpleName} - fetchApiDefinition")
    http.GET[Option[ApiDefinition]](definitionUrl(serviceName)) recover recovery
  }

  def fetchApiCategoryDetails()(implicit hc: HeaderCarrier): Future[List[ApiCategoryDetails]] = {
    logger.info(s"${this.getClass.getSimpleName} - fetchApiCategoryDetails")
    http.GET[List[ApiCategoryDetails]](categoriesUrl) recover recovery
  }

  def fetchApiDocumentationResource(resourceId: ResourceId)(implicit hc: HeaderCarrier): Future[Option[WSResponse]]

  def fetchApiSpecification(serviceName: String, version: ApiVersion)(implicit hc: HeaderCarrier): Future[Option[JsValue]] = {
    logger.info(s"${this.getClass.getSimpleName} - fetchApiSpecification")
    http.GET[Option[JsObject]](specificationUrl(serviceName, version)) recover recovery
  }

}
