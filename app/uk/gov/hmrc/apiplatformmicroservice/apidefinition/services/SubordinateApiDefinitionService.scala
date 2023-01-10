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

package uk.gov.hmrc.apiplatformmicroservice.apidefinition.services

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.connectors.SubordinateApiDefinitionConnector
import uk.gov.hmrc.apiplatformmicroservice.metrics.{API, ApiMetrics}
import uk.gov.hmrc.apiplatformmicroservice.common.ApplicationLogger

@Singleton
class SubordinateApiDefinitionService @Inject() (
    val connector: SubordinateApiDefinitionConnector,
    val config: SubordinateApiDefinitionService.Config,
    val apiMetrics: ApiMetrics
  ) extends ApiDefinitionService with ApplicationLogger {

  val api: API = API("api-definition-subordinate")

  val enabled: Boolean = config.enabled

  logger.info(s"Subordinate Api Definition Service is ${if (enabled) "enabled" else "disabled"}")
}

object SubordinateApiDefinitionService {
  case class Config(enabled: Boolean)
}
