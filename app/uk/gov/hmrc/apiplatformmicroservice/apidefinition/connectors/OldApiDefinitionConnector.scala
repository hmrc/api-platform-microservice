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
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.connectors.OldApiDefinitionConnector.{combinedDefinitionUrl, definitionsUrl}
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.JsonFormatters._
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.{APIDefinition, CombinedAPIDefinition}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

@Singleton
private[apidefinition] class OldApiDefinitionConnector @Inject()(http: HttpClient, val config: ApiDefinitionConnectorConfig)
                                                                (implicit val ec: ExecutionContext) {

  def fetchAllApiDefinitions(implicit hc: HeaderCarrier): Future[Seq[APIDefinition]] = {
    Logger.info(s"${this.getClass.getSimpleName} - fetchAllApiDefinitions")
    http.GET[Seq[APIDefinition]](definitionsUrl(config.baseUrl), Seq("filterApis" -> "false"))
  }

  def fetchCombinedApiDefinition(serviceName: String)(implicit hc: HeaderCarrier): Future[CombinedAPIDefinition] = {
    Logger.info(s"${this.getClass.getSimpleName} - fetchApiDefinition")
    http.GET[CombinedAPIDefinition](combinedDefinitionUrl(config.baseUrl, serviceName))
  }
}

private[apidefinition] object OldApiDefinitionConnector {
  def definitionsUrl(serviceBaseUrl: String) = s"$serviceBaseUrl/api-definition"
  def combinedDefinitionUrl(serviceBaseUrl: String, serviceName: String) = s"$serviceBaseUrl/api-definition/$serviceName"
}

private[apidefinition] case class ApiDefinitionConnectorConfig(baseUrl: String)
