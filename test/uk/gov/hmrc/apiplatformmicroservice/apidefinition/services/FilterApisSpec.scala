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

package uk.gov.hmrc.apiplatformmicroservice.apidefinition.services

import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatformmicroservice.util.HmrcSpec
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiDefinitionTestDataHelper
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.APIDefinition
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiIdentifier

class FilterApisSpec extends HmrcSpec with ApiDefinitionTestDataHelper {
  private val appId = ApplicationId.random

  private val filter = new FilterApis {}
  private val api = apiDefinition("test")
  private val apiId = ApiIdentifier(api.context, apiVersion().version)
  private val publicApi = apiDefinition("test", apiVersion().asStable.asPublic)
  private val privateApi = apiDefinition("test", apiVersion().asStable.asPrivate)
  private val privateTrialApi = privateApi.asTrial
  private val privateAllowListApi = apiDefinition("test", apiVersion().asStable.asPrivate.addAllowList(appId))

  private val allPublicApis = Seq(
    publicApi.asAlpha, 
    publicApi.asBeta, 
    publicApi.asStable, 
    publicApi.asDeprecated, 
    publicApi.asRetired
  )
  
  private val allPrivateAllowListApis = Seq(
    privateAllowListApi.asAlpha, 
    privateAllowListApi.asBeta, 
    privateAllowListApi.asStable, 
    privateAllowListApi.asDeprecated, 
    privateAllowListApi.asRetired
  )

  private val allPrivateTrialApis = Seq(
    privateApi.asTrial.asAlpha, 
    privateApi.asTrial.asBeta, 
    privateApi.asTrial.asStable, 
    privateApi.asTrial.asDeprecated, 
    privateApi.asTrial.asRetired
  )

  private val allPrivateApis = Seq(
    privateApi.asAlpha, 
    privateApi.asBeta, 
    privateApi.asStable, 
    privateApi.asDeprecated, 
    privateApi.asRetired
  )


  "filterApisForDevHubSubscription" when {
    def testFilterSubs(subscription: ApiIdentifier)(apiDefinitions: APIDefinition*): Seq[APIDefinition] = {
      filter.filterApisForDevHubSubscription(Set(appId), Set(subscription))(apiDefinitions.toSeq)
    }

    def testFilter(apiDefinitions: APIDefinition*): Seq[APIDefinition] = {
      filter.filterApisForDevHubSubscription(Set(appId), Set.empty)(apiDefinitions.toSeq)
    }

    "filtering public api" should {
      "allow beta and stable" in {
        testFilter(allPublicApis:_*) should contain only (publicApi.asBeta, publicApi.asStable)
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
        testFilter(allPrivateApis:_*) shouldBe empty
       }

      "allow when subscribed" in {
        testFilterSubs(apiId)(allPrivateApis:_*) should contain only(
          privateApi.asBeta, 
          privateApi.asStable, 
          privateApi.asDeprecated
        )
      } 
    }

    "filtering private trial apis where the app is not in the allow list" should {

      "reject any state when not subscribed" in {
        testFilter(allPrivateTrialApis:_*) shouldBe empty
      }

      "allow when subscribed" in {
        testFilterSubs(apiId)(allPrivateTrialApis:_*) should contain only(
          privateApi.asTrial.asBeta, 
          privateApi.asTrial.asStable, 
          privateApi.asTrial.asDeprecated
        )
      }
    }

    "filtering private apis where the app is in the allow list" should {
      "allow when not subscribed for beta or stable" in {
        testFilter(allPrivateAllowListApis:_*) should contain only (privateAllowListApi.asBeta, privateAllowListApi.asStable)
      }
      
      "allow when subscribed for all states" in {
        testFilterSubs(apiId)(allPrivateAllowListApis:_*) should contain only (
          privateAllowListApi.asBeta, 
          privateAllowListApi.asStable, 
          privateAllowListApi.asDeprecated
        )
      }
    }
  }

  "filterApisForGateKeeperSubscription" when {
    def testFilter(apiDefinitions: APIDefinition*): Seq[APIDefinition] = {
      filter.filterApisForGateKeeperSubscription(Set(appId), Set.empty)(apiDefinitions.toSeq)
    }

    "filtering public api" should {
      "allow all but retired" in {
        testFilter(allPublicApis:_*) should contain only (
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

    "filtering private apis where the app is not in the allow list" should {
      "allow all except retired" in {
        testFilter(allPrivateApis:_*) should contain only (
          privateApi.asAlpha,
          privateApi.asBeta,
          privateApi.asStable,
          privateApi.asDeprecated
        )
      }

      "reject retired" in {
        testFilter(privateApi.asRetired) shouldBe empty
      }
    }

    "filtering private trial apis where the app is not in the allow list" should {
      "allow all except retired" in {
        testFilter(allPrivateTrialApis:_*) should contain only (
          privateTrialApi.asAlpha,
          privateTrialApi.asBeta,
          privateTrialApi.asStable,
          privateTrialApi.asDeprecated
        )
      }

      "reject retired" in {
        testFilter(privateTrialApi.asRetired) shouldBe empty
      }
    }

    "filtering private apis where the app is in the allow list" should {
    }
  }

  "filterApisForDocumentation" when {
    def testFilterSubs(subscription: ApiIdentifier)(apiDefinitions: APIDefinition*): Seq[APIDefinition] = {
      filter.filterApisForDocumentation(Set(appId), Set(subscription))(apiDefinitions.toSeq)
    }

    def testFilter(apiDefinitions: APIDefinition*): Seq[APIDefinition] = {
      filter.filterApisForDocumentation(Set(appId), Set.empty)(apiDefinitions.toSeq)
    }

    "filtering public api" should {
      "allow alpha, beta and stable" in {
        testFilter(allPublicApis:_*) should contain only (publicApi.asAlpha, publicApi.asBeta, publicApi.asStable)
      }

      "reject retired" in {
        testFilter(api.withVersions(apiVersion().asRetired)) shouldBe empty
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
        testFilter(allPrivateApis:_*) shouldBe empty
       }

      "allow when subscribed" in {
        testFilterSubs(apiId)(allPrivateApis:_*) should contain only(
          privateApi.asAlpha,
          privateApi.asBeta, 
          privateApi.asStable, 
          privateApi.asDeprecated
        )
      } 
    }

    "filtering private trial apis where the app is not in the allow list" should {
      // Note - the DocFe will only show the summary docs unless allow listed/ subscribed
      "allow alpha, beta and stable when not subscribed" in {
        testFilter(allPrivateTrialApis:_*) should contain only (
          privateApi.asTrial.asAlpha, 
          privateApi.asTrial.asBeta, 
          privateApi.asTrial.asStable
        )
      }

      "allow when subscribed" in {
        testFilterSubs(apiId)(allPrivateTrialApis:_*) should contain only (
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

    "filtering private apis where the app is in the allow list" should {
      "allow only alpha, beta or stable when not subscribed" in {
        testFilter(allPrivateAllowListApis:_*) should contain only (privateAllowListApi.asAlpha, privateAllowListApi.asBeta, privateAllowListApi.asStable)
      }
      
      "allow when subscribed for all states" in {
        testFilterSubs(apiId)(allPrivateAllowListApis:_*) should contain only (
          privateAllowListApi.asAlpha,
          privateAllowListApi.asBeta, 
          privateAllowListApi.asStable, 
          privateAllowListApi.asDeprecated
        )
      }
    }
  }

}
