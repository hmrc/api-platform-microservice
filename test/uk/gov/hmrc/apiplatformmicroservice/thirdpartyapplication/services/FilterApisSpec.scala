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
  val publicApi = apiDefinition("test", apiVersion().asStable.asPublic)
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
        testFilter(
          publicApi.asAlpha, 
          publicApi.asBeta, 
          publicApi.asStable, 
          publicApi.asDeprecated, 
          publicApi.asRetired
        ) should contain only (publicApi.asBeta, publicApi.asStable)
      }

      "reject retired" in {
        testFilter(api.withVersions(apiVersion().asRetired)) shouldBe empty
      }
      "reject alpha when not subscribed to" in {
        testFilter(publicApi.asAlpha) shouldBe empty
      }
      "reject deprecated not subscribed to" in {
        testFilter(publicApi.asDeprecated) shouldBe empty
      }
      "allow deprecated and alpha when subscribed to" in {
        testFilter(apiId)(publicApi.asDeprecated, publicApi.asAlpha) should contain only (publicApi.asDeprecated, publicApi.asAlpha)
      }
    }

    "filtering private apis where the app is not in the allow list" should {
      val allPrivateApis = Seq(
          privateApi.asAlpha, 
          privateApi.asBeta, 
          privateApi.asStable, 
          privateApi.asDeprecated, 
          privateApi.asRetired
      )

      "reject any state" in {
        testFilter(allPrivateApis:_*) shouldBe empty
       }

      "allow when subscribed" in {
        testFilter(apiId)(allPrivateApis:_*) should contain only(
          privateApi.asAlpha,       // Currently an illegal state that some Apps find themselves in.
          privateApi.asBeta, 
          privateApi.asStable, 
          privateApi.asDeprecated
        )
      } 
    }

    "filtering private trial apis where the app is not in the allow list" should {
      val allPrivateTrialApis = Seq(
        privateApi.asTrial.asAlpha, 
        privateApi.asTrial.asBeta, 
        privateApi.asTrial.asStable, 
        privateApi.asTrial.asDeprecated, 
        privateApi.asTrial.asRetired
      )

      "reject any state when not subscribed" in {
        testFilter(allPrivateTrialApis:_*) shouldBe empty
      }

      "allow when subscribed" in {
        testFilter(apiId)(allPrivateTrialApis:_*) should contain only(
          privateApi.asTrial.asAlpha,  // Currently an illegal state that some Apps find themselves in.
          privateApi.asTrial.asBeta, 
          privateApi.asTrial.asStable, 
          privateApi.asTrial.asDeprecated
        )
      }
    }

    "filtering private apis where the app is in the allow list" should {
      val allPrivateAllowListApis = Seq(
          privateAllowListApi.asAlpha, 
          privateAllowListApi.asBeta, 
          privateAllowListApi.asStable, 
          privateAllowListApi.asDeprecated, 
          privateAllowListApi.asRetired
      )

      "allow when not subscribed for beta or stable" in {
        testFilter(allPrivateAllowListApis:_*) should contain only (privateAllowListApi.asBeta, privateAllowListApi.asStable)
      }
      
      "allow when subscribed for all states" in {
        testFilter(apiId)(allPrivateAllowListApis:_*) should contain only (
          privateAllowListApi.asAlpha, 
          privateAllowListApi.asBeta, 
          privateAllowListApi.asStable, 
          privateAllowListApi.asDeprecated
        )
      }
    }
  }
}
