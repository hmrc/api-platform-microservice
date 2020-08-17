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

import play.api.Logger
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.{APIDefinition, JsonFormatters, ResourceId}
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.http.ws.WSGet

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

trait ApiDefinitionConnector extends ApiDefinitionConnectorUtils with JsonFormatters {
  def http: HttpClient with WSGet
  def serviceBaseUrl: String
  implicit val ec: ExecutionContext

  def fetchAllApiDefinitions(implicit hc: HeaderCarrier): Future[Seq[APIDefinition]] = {
    Logger.info(s"${this.getClass.getSimpleName} - fetchAllApiDefinitionsWithoutFiltering")
    val r = http.GET[Seq[APIDefinition]](definitionsUrl(serviceBaseUrl), Seq("type" -> "all"))

    r.foreach(_.foreach(defn => Logger.info(s"Found ${defn.name}")))

    r.map(e => e.sortBy(_.name))
      .recover {
        case _: NotFoundException =>
          Logger.info("Not found")
          Seq.empty
        case NonFatal(e)          =>
          Logger.error(s"Failed $e")
          throw e
      }
  }

  def fetchApiDefinition(serviceName: String)(implicit hc: HeaderCarrier): Future[Option[APIDefinition]] = {
    Logger.info(s"${this.getClass.getSimpleName} - fetchApiDefinition")
    val r = http.GET[APIDefinition](definitionUrl(serviceBaseUrl, serviceName))

    r.map(Some(_))
      .recover {
        case _: NotFoundException =>
          Logger.info("Not found")
          None
        case NonFatal(e)          =>
          Logger.error(s"Failed $e")
          throw e
      }
  }

  def fetchApiDocumentationResource(resourceId: ResourceId)(implicit hc: HeaderCarrier): Future[Option[WSResponse]]
}
