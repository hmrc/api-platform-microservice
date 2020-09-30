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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services

import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.services.FilterApis
import uk.gov.hmrc.apiplatformmicroservice.util.HmrcSpec
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiDefinitionTestDataHelper
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.APIDefinition
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiIdentifier

class FilterApisSpec extends HmrcSpec with ApiDefinitionTestDataHelper {
  val appId = ApplicationId.random

  val filter = new FilterApis {}
  val api = apiDefinition("test")
  val apiId = ApiIdentifier(api.context, apiVersion().version)
  val publicStableApi = apiDefinition("test", apiVersion().asStable.asPublic)
  val publicDeprecatedApi = api.withVersions(apiVersion().asDeprecated)
  val privateApi = apiDefinition("test", apiVersion().asStable.asPrivate)
  val privateAllowListApi = apiDefinition("test", apiVersion().asStable.asPrivate.addAllowList(appId))
  val privateTrialApi = apiDefinition("test", apiVersion().asStable.asTrial)

  def testFilter(subscriptions: ApiIdentifier*)(apiDefinitions: APIDefinition*): Seq[APIDefinition] = {
    filter.filterApis(appId, subscriptions.toSet)(apiDefinitions.toSeq)
  }
  def testFilter(apiDefinitions: APIDefinition*): Seq[APIDefinition] = {
    filter.filterApis(appId, Set.empty)(apiDefinitions.toSeq)
  }

  "filterApis" when {
    "filtering public api" should {
      "allow stable" in {
        testFilter(publicStableApi) should contain only publicStableApi
      }
      "reject retired" in {
        testFilter(api.withVersions(apiVersion().asRETIRED)) shouldBe empty
      }
      "reject deprecated not subscribed to" in {
        testFilter(publicDeprecatedApi) shouldBe empty
      }
      "allow deprecated but subscribed to" in {
        testFilter(apiId)(publicDeprecatedApi) should contain only publicDeprecatedApi
      }
    }

    "filtering private apis where the app is not in the allow list" should {
      "reject any state" in {
        testFilter(
          privateApi, 
          privateApi.asAlpha, 
          privateApi.asBeta, 
          privateApi.asDeprecated, 
          privateApi.asRETIRED
        ) shouldBe empty
      }
    }
    "filtering private apis where the app is in the allow list" should {
      "allow stable" in {
        testFilter(privateAllowListApi) should contain only privateAllowListApi
      }
      "allow where the app is in the allow list unless either retired or deprecated but not subscribed" in {
        testFilter(
          privateAllowListApi, 
          privateAllowListApi.asAlpha, 
          privateAllowListApi.asBeta, 
          privateAllowListApi.asDeprecated, 
          privateAllowListApi.asRETIRED
        ) should contain allOf(privateAllowListApi.asAlpha, privateAllowListApi, privateAllowListApi.asBeta)
      }
      "allow where the app is in the allow list and deprecated but also subscribed" in {
        val local = privateAllowListApi.asDeprecated
        testFilter(apiId)(local) should contain only local
      }
    }
  }
}
