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

import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiDefinitionTestDataHelper

class FilterApisSpec extends FilterApisSpecHelper with ApiDefinitionTestDataHelper with FilterApiDocumentation {
  "filterApisForDocumentation" when {
    val filter = new FilterApiDocumentation {}

    def testFilterSubs(subscription: ApiIdentifier)(apiDefinitions: ApiDefinition*): List[ApiDefinition] = {
      filter.filterApisForDocumentation(Set(subscription))(apiDefinitions.toList)
    }

    def testFilter(apiDefinitions: ApiDefinition*): List[ApiDefinition] = {
      filter.filterApisForDocumentation(Set.empty)(apiDefinitions.toList)
    }

    "filtering public api" should {
      "allow alpha, beta and stable" in {
        testFilter(allPublicApis: _*) should contain.only(publicApi.asAlpha, publicApi.asBeta, publicApi.asStable)
      }

      "reject retired" in {
        testFilter(api.withVersions(apiVersion().asRetired)) shouldBe empty
      }

      "reject deprecated not subscribed to" in {
        testFilter(publicApi.asDeprecated) shouldBe empty
      }

      "allow deprecated when subscribed to" in {
        testFilterSubs(apiId)(publicApi.asDeprecated) should contain.only(publicApi.asDeprecated)
      }
    }

    "filtering private apis" should {
      "reject any state" in {
        testFilter(allPrivateApis: _*) shouldBe empty
      }

      "allow when subscribed" in {
        testFilterSubs(apiId)(allPrivateApis: _*) should contain.only(
          privateApi.asAlpha,
          privateApi.asBeta,
          privateApi.asStable,
          privateApi.asDeprecated
        )
      }
    }

    "filtering private trial apis" should {
      // Note - the DocFe will only show the summary docs unless allow listed/ subscribed
      "allow alpha, beta and stable when not subscribed" in {
        testFilter(allPrivateTrialApis: _*) should contain.only(
          privateApi.asTrial.asAlpha,
          privateApi.asTrial.asBeta,
          privateApi.asTrial.asStable
        )
      }

      "allow when subscribed" in {
        testFilterSubs(apiId)(allPrivateTrialApis: _*) should contain.only(
          privateApi.asTrial.asAlpha,
          privateApi.asTrial.asBeta,
          privateApi.asTrial.asStable,
          privateApi.asTrial.asDeprecated
        )
      }

      "reject retired" in {
        testFilter(privateTrialApi.asRetired) shouldBe empty
      }
    }
  }
}
