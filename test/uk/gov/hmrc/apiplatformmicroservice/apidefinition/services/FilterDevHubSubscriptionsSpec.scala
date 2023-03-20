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

import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiDefinition
import uk.gov.hmrc.apiplatform.modules.apis.domain.models._

class FilterDevHubSubscriptionsSpec extends FilterApisSpecHelper with FilterDevHubSubscriptions {

  private val filter = new FilterDevHubSubscriptions {}

  "FilterDevHubSubscriptions" when {

    def testFilterSubs(subscription: ApiIdentifier)(apiDefinitions: ApiDefinition*): List[ApiDefinition] = {
      filter.filterApisForDevHubSubscriptions(Set(appId), Set(subscription))(apiDefinitions.toList)
    }

    def testFilter(apiDefinitions: ApiDefinition*): List[ApiDefinition] = {
      filter.filterApisForDevHubSubscriptions(Set(appId), Set.empty)(apiDefinitions.toList)
    }

    "filtering public api" should {
      "allow beta and stable" in {
        testFilter(allPublicApis: _*) should contain only (publicApi.asBeta, publicApi.asStable)
      }

      "reject retired" in {
        testFilter(api.withVersions(apiVersion().asRetired)) shouldBe empty
      }
      "reject alpha when not subscribed to" in {
        testFilter(publicApi.asAlpha) shouldBe empty
      }
      "reject alpha when subscribed to" in {
        testFilterSubs(apiId)(publicApi.asAlpha) shouldBe empty
      }
      "reject deprecated not subscribed to" in {
        testFilter(publicApi.asDeprecated) shouldBe empty
      }
      "allow deprecated when subscribed to" in {
        testFilterSubs(apiId)(publicApi.asDeprecated) should contain only (publicApi.asDeprecated)
      }
    }

    "filtering private apis where the app is not in the allow list" should {

      "reject any state" in {
        testFilter(allPrivateApis: _*) shouldBe empty
      }

      "allow when subscribed" in {
        testFilterSubs(apiId)(allPrivateApis: _*) should contain only (
          privateApi.asBeta,
          privateApi.asStable,
          privateApi.asDeprecated
        )
      }
    }

    "filtering private trial apis where the app is not in the allow list" should {

      "reject any state when not subscribed" in {
        testFilter(allPrivateTrialApis: _*) shouldBe empty
      }

      "allow when subscribed" in {
        testFilterSubs(apiId)(allPrivateTrialApis: _*) should contain only (
          privateApi.asTrial.asBeta,
          privateApi.asTrial.asStable,
          privateApi.asTrial.asDeprecated
        )
      }
    }

    "filtering private apis where the app is in the allow list" should {
      "allow when not subscribed for beta or stable" in {
        testFilter(allPrivateAllowListApis: _*) should contain only (privateAllowListApi.asBeta, privateAllowListApi.asStable)
      }

      "allow when subscribed for all states" in {
        testFilterSubs(apiId)(allPrivateAllowListApis: _*) should contain only (
          privateAllowListApi.asBeta,
          privateAllowListApi.asStable,
          privateAllowListApi.asDeprecated
        )
      }
    }
  }

}
