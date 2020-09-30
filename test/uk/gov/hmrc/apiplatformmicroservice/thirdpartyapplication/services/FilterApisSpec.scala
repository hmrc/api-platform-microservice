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
        testFilter(
          publicApi.asAlpha, 
          publicApi.asBeta, 
          publicApi.asStable, 
          publicApi.asDeprecated, 
          publicApi.asRETIRED
        ) should contain only (publicApi.asAlpha, publicApi.asBeta, publicApi.asStable)
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
      val allPrivateApis = Seq(
          privateApi.asAlpha, 
          privateApi.asBeta, 
          privateApi.asStable, 
          privateApi.asDeprecated, 
          privateApi.asRETIRED
      )

      "reject any state" in {
        testFilter(allPrivateApis:_*) shouldBe empty
       }

      // Currently an illegal state that some Apps find themselves in.
      "allow when subscribed" in {
        testFilter(apiId)(allPrivateApis:_*) should contain only(
          privateApi.asAlpha, 
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
        privateApi.asTrial.asRETIRED
      )

      "reject any state" in {
        testFilter(allPrivateTrialApis:_*) shouldBe empty
      }

      // Currently an illegal state that some Apps find themselves in.
      "show when subscribed" in {
        testFilter(apiId)(allPrivateTrialApis:_*) should contain only(
          privateApi.asTrial.asAlpha, 
          privateApi.asTrial.asBeta, 
          privateApi.asTrial.asStable, 
          privateApi.asTrial.asDeprecated, 
        )
      }
    }

    "filtering private apis where the app is in the allow list" should {
      "allow stable" in {
        testFilter(privateAllowListApi) should contain only privateAllowListApi
      }
      "allow where the app is in the allow list unless either retired or deprecated but not subscribed" in {
        testFilter(
          privateAllowListApi.asAlpha, 
          privateAllowListApi.asBeta, 
          privateAllowListApi.asStable, 
          privateAllowListApi.asDeprecated, 
          privateAllowListApi.asRETIRED
        ) should contain allOf(privateAllowListApi.asAlpha, privateAllowListApi.asBeta, privateAllowListApi.asStable)
      }
      "allow where the app is in the allow list and deprecated but also subscribed" in {
        val local = privateAllowListApi.asDeprecated
        testFilter(apiId)(local) should contain only local
      }
    }
  }
}


// ArrayBuffer(
  
//   APIDefinition(test,test,test,ApiContext(test),false,false,ArrayBuffer(ApiVersionDefinition(ApiVersion(1.0),ALPHA,PublicApiAccess(),NonEmptyList(Endpoint(Today's Date,/today,GET,List()), Endpoint(Yesterday's Date,/yesterday,GET,List())),false)),List()), 
//   APIDefinition(test,test,test,ApiContext(test),false,false,ArrayBuffer(ApiVersionDefinition(ApiVersion(1.0),BETA,PublicApiAccess(),NonEmptyList(Endpoint(Today's Date,/today,GET,List()), Endpoint(Yesterday's Date,/yesterday,GET,List())),false)),List()), 
//   APIDefinition(test,test,test,ApiContext(test),false,false,ArrayBuffer(ApiVersionDefinition(ApiVersion(1.0),STABLE,PublicApiAccess(),NonEmptyList(Endpoint(Today's Date,/today,GET,List()), Endpoint(Yesterday's Date,/yesterday,GET,List())),false)),List())) 
//   did not contain only (
//   APIDefinition(test,test,test,ApiContext(test),false,false,ArrayBuffer(ApiVersionDefinition(ApiVersion(1.0),BETA,PublicApiAccess(),NonEmptyList(Endpoint(Today's Date,/today,GET,List()), Endpoint(Yesterday's Date,/yesterday,GET,List())),false)),List()), 
//   APIDefinition(test,test,test,ApiContext(test),false,false,ArrayBuffer(ApiVersionDefinition(ApiVersion(1.0),STABLE,PublicApiAccess(),NonEmptyList(Endpoint(Today's Date,/today,GET,List()), Endpoint(Yesterday's Date,/yesterday,GET,List())),false)),List()))
//   (FilterApisSpec.scala:48)