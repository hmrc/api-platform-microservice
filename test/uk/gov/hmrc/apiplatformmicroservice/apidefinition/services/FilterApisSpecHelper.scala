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

import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ApiIdentifier
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.ApplicationId
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiDefinitionTestDataHelper
import uk.gov.hmrc.apiplatformmicroservice.common.utils.HmrcSpec

class FilterApisSpecHelper extends HmrcSpec with ApiDefinitionTestDataHelper {
  protected val appId = ApplicationId.random

  protected val api                 = apiDefinition("test")
  protected val apiId               = ApiIdentifier(api.context, apiVersion().version)
  protected val publicApi           = apiDefinition("test", apiVersion().asStable.asPublic)
  protected val privateApi          = apiDefinition("test", apiVersion().asStable.asPrivate)
  protected val privateTrialApi     = privateApi.asTrial
  protected val privateAllowListApi = apiDefinition("test", apiVersion().asStable.asPrivate.addAllowList(appId))

  protected val allPublicApis = List(
    publicApi.asAlpha,
    publicApi.asBeta,
    publicApi.asStable,
    publicApi.asDeprecated,
    publicApi.asRetired
  )

  protected val allPrivateAllowListApis = List(
    privateAllowListApi.asAlpha,
    privateAllowListApi.asBeta,
    privateAllowListApi.asStable,
    privateAllowListApi.asDeprecated,
    privateAllowListApi.asRetired
  )

  protected val allPrivateTrialApis = List(
    privateApi.asTrial.asAlpha,
    privateApi.asTrial.asBeta,
    privateApi.asTrial.asStable,
    privateApi.asTrial.asDeprecated,
    privateApi.asTrial.asRetired
  )

  protected val allPrivateApis = List(
    privateApi.asAlpha,
    privateApi.asBeta,
    privateApi.asStable,
    privateApi.asDeprecated,
    privateApi.asRetired
  )
}
