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

package uk.gov.hmrc.apiplatformmicroservice.utils

import play.api.Configuration

trait ConfigBuilder {

  protected def stubConfig(wiremockPrincipalPort: Int, wiremockSubordinatePort: Int) = Configuration(
    "microservice.services.api-definition-principal.port" -> wiremockPrincipalPort,
    "microservice.services.api-definition-subordinate.port" -> wiremockSubordinatePort,
    "microservice.services.third-party-application-principal.port" -> wiremockPrincipalPort,
    "microservice.services.third-party-application-subordinate.port" -> wiremockSubordinatePort,
    "microservice.services.subscription-fields-principal.port" -> wiremockPrincipalPort,
    "microservice.services.subscription-fields-subordinate.port" -> wiremockSubordinatePort,
    "microservice.services.api-platform-xml-services.port" -> wiremockPrincipalPort,
    "metrics.jvm" -> false
  )

}
