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

import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ApiDefinition

class FilterGateKeeperSubscriptionsSpec extends FilterApisSpecHelper with FilterGateKeeperSubscriptions {

  private val filter = new FilterGateKeeperSubscriptions {}

  "filterApisForGateKeeperSubscription" when {
    def testFilter(apiDefinitions: ApiDefinition*): List[ApiDefinition] = {
      filter.filterApisForGateKeeperSubscriptions(apiDefinitions.toList)
    }

    "filtering public api" should {
      "allow all but retired" in {
        testFilter(allPublicApis: _*) should contain.only(
          publicApi.asAlpha,
          publicApi.asBeta,
          publicApi.asStable,
          publicApi.asDeprecated
        )
      }

      "reject retired" in {
        testFilter(api.withVersions(apiVersion().asRetired)) shouldBe empty
      }
    }

    "filtering non-public apis where the app is not in the allow list" should {
      "allow all except retired" in {
        testFilter(allInternalApis: _*) should contain.only(
          internalApi.asAlpha,
          internalApi.asBeta,
          internalApi.asStable,
          internalApi.asDeprecated
        )
      }

      "reject retired" in {
        testFilter(internalApi.asRetired) shouldBe empty
      }
    }

    "filtering controlled apis where the app is not in the allow list" should {
      "allow all except retired" in {
        testFilter(allControlledApis: _*) should contain.only(
          controlledApi.asAlpha,
          controlledApi.asBeta,
          controlledApi.asStable,
          controlledApi.asDeprecated
        )
      }

      "reject retired" in {
        testFilter(controlledApi.asRetired) shouldBe empty
      }
    }

    "filtering non-public apis where the app is in the allow list" should {}
  }
}
