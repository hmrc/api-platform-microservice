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

import javax.inject.{Inject, Singleton}
import play.api.Logger
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.connectors.ApiDefinitionConnector.{ApiDefinitionConnectorConfig, definitionsUrl}
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.APIDefinition
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.JsonFormatters._
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException, Upstream5xxResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApiDefinitionConnector @Inject()(http: HttpClient, val config: ApiDefinitionConnectorConfig)
                                      (implicit val ec: ExecutionContext) {

  def fetchAllApiDefinitions(implicit hc: HeaderCarrier): Future[Seq[APIDefinition]] = {
    Logger.info(s"${this.getClass.getSimpleName} - fetchAllApiDefinitions")
    http.GET[Seq[APIDefinition]](definitionsUrl(config.baseUrl), Seq("filterApis" -> "false")) recover {
      case _ : NotFoundException => { Logger.info("Not found"); Seq.empty}
      case e : Upstream5xxResponse => { Logger.error(s"Failed $e"); throw e}
      case e => { Logger.error(s"Failed $e"); throw e}
    }
  }
}

object ApiDefinitionConnector {
  case class ApiDefinitionConnectorConfig(baseUrl: String)
  def definitionsUrl(serviceBaseUrl: String) = s"$serviceBaseUrl/api-definition"
}
