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
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ResourceId
import uk.gov.hmrc.apiplatformmicroservice.common.ApplicationLogger
import uk.gov.hmrc.apiplatformmicroservice.common.connectors.ConnectorRecovery

trait ApiDefinitionConnector extends ApiDefinitionConnectorUtils
    with ApplicationLogger with ConnectorRecovery {

  def http: HttpClientV2
  def serviceBaseUrl: String
  implicit val ec: ExecutionContext

  def configureEbridgeIfRequired: RequestBuilder => RequestBuilder

  def fetchAllApiDefinitions(implicit hc: HeaderCarrier): Future[List[ApiDefinition]] = {
    logger.info(s"${this.getClass.getSimpleName} - fetchAllApiDefinitionsWithoutFiltering")
    configureEbridgeIfRequired(
      http.get(url"$definitionsUrl?type=all")
    )
      .execute[Option[List[ApiDefinition]]]
      .map(_ match {
        case None                 => List.empty
        case Some(apiDefinitions) => apiDefinitions.sortBy(_.name)
      })
  }

  def fetchApiDefinition(serviceName: ServiceName)(implicit hc: HeaderCarrier): Future[Option[ApiDefinition]] = {
    logger.info(s"${this.getClass.getSimpleName} - fetchApiDefinition")
    configureEbridgeIfRequired(
      http.get(definitionUrl(serviceName))
    )
      .execute[Option[ApiDefinition]]
      .recover(recovery)
  }

  def fetchApiDocumentationResource(resourceId: ResourceId)(implicit hc: HeaderCarrier): Future[Option[HttpResponse]]

  def fetchApiSpecification(serviceName: ServiceName, version: ApiVersionNbr)(implicit hc: HeaderCarrier): Future[Option[JsValue]] = {
    logger.info(s"${this.getClass.getSimpleName} - fetchApiSpecification")
    configureEbridgeIfRequired(
      http.get(specificationUrl(serviceName, version))
    )
      .execute[Option[JsObject]]
      .recover(recovery)
  }

}
