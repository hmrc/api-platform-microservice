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

import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ResourceId

trait ApiDefinitionConnectorUtils {

  def definitionsUrl(serviceBaseUrl: String) = s"$serviceBaseUrl/api-definition"

  def definitionUrl(serviceBaseUrl: String, serviceName: String) =
    s"$serviceBaseUrl/api-definition/$serviceName"

  def documentationUrl(serviceBaseUrl: String,
                       resourceId: ResourceId): String = {
    import resourceId._
    s"$serviceBaseUrl/api-definition/$serviceName/$version/documentation/$resource"
  }
}

object ApiDefinitionConnectorUtils extends ApiDefinitionConnectorUtils
