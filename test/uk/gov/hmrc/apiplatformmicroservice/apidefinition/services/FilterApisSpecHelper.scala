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

import uk.gov.hmrc.apiplatform.modules.common.domain.models.*
import uk.gov.hmrc.apiplatform.modules.common.utils
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiDefinitionTestDataHelper

class FilterApisSpecHelper extends utils.HmrcSpec with ApiDefinitionTestDataHelper {
  protected val appId = ApplicationId.random

  protected val api           = apiDefinition("test")
  protected val apiId         = ApiIdentifier(api.context, apiVersion().versionNbr)
  protected val publicApi     = apiDefinition("test", apiVersion().asStable.asPublic)
  protected val internalApi   = apiDefinition("test", apiVersion().asStable.asInternal)
  protected val controlledApi = internalApi.asControlled

  protected val allPublicApis = List(
    publicApi.asAlpha,
    publicApi.asBeta,
    publicApi.asStable,
    publicApi.asDeprecated,
    publicApi.asRetired
  )

  protected val allControlledApis = List(
    controlledApi.asAlpha,
    controlledApi.asBeta,
    controlledApi.asStable,
    controlledApi.asDeprecated,
    controlledApi.asRetired
  )

  protected val allInternalApis = List(
    internalApi.asAlpha,
    internalApi.asBeta,
    internalApi.asStable,
    internalApi.asDeprecated,
    internalApi.asRetired
  )
}
