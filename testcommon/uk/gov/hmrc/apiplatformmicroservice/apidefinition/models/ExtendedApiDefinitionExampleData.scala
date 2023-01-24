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

package uk.gov.hmrc.apiplatformmicroservice.apidefinition.models

import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiStatus.STABLE
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ApiVersion

trait ExtendedApiDefinitionExampleData {
  self: ApiDefinitionTestDataHelper =>

  val apiName    = "hello-api"
  val versionOne = ApiVersion("1.0")

  val anExtendedApiDefinitionWithOnlySubordinate = extendedApiDefinition(
    apiName,
    List(extendedApiVersion(versionOne, STABLE, None, Some(ApiAvailability(endpointsEnabled = true, PublicApiAccess(), loggedIn = true, authorised = true))))
  )

  val anExtendedApiDefinitionWithOnlyPrincipal = extendedApiDefinition(
    apiName,
    List(
      extendedApiVersion(
        versionOne,
        STABLE,
        Some(
          ApiAvailability(
            endpointsEnabled = true,
            PublicApiAccess(),
            loggedIn = true,
            authorised = true
          )
        ),
        None
      )
    )
  )

  val anExtendedApiDefinitionWithPrincipalAndSubordinate = extendedApiDefinition(
    apiName,
    List(extendedApiVersion(
      versionOne,
      STABLE,
      Some(ApiAvailability(endpointsEnabled = true, PublicApiAccess(), loggedIn = true, authorised = true)),
      Some(ApiAvailability(endpointsEnabled = true, PublicApiAccess(), loggedIn = true, authorised = true))
    ))
  )
}
