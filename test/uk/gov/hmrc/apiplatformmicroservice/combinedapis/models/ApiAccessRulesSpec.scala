/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.apiplatformmicroservice.combinedapis.models

import cats.data.NonEmptyList
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.{ApiAccess, ApiAccessRules, ApiAccessType, ApiContext, ApiDefinition, ApiStatus, ApiVersion, ApiVersionDefinition, AuthType, Endpoint, HttpMethod, PrivateApiAccess, PublicApiAccess}
import uk.gov.hmrc.apiplatformmicroservice.combinedapis.utils.CombinedApiDataHelper
import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec

class ApiAccessRulesSpec extends AsyncHmrcSpec with ApiAccessRules {

  val endpoint1 = Endpoint("endpoint1", "/some/endpoint1", HttpMethod.POST, AuthType.USER)
  def versionDefinition(version : String, status: ApiStatus, apiAccess: ApiAccess) ={
    ApiVersionDefinition(ApiVersion(version), status, apiAccess, NonEmptyList.fromListUnsafe(List(endpoint1)))
  }

  def newDefinition(versions: List[ApiVersionDefinition]) ={
    ApiDefinition("test1ServiceName", "test1Name", "test1Desc", ApiContext("som/context/here"), requiresTrust = false, isTestSupport = false,  versions )
  }

  val allPublicVersions: List[ApiVersionDefinition] = List( versionDefinition("1.0", ApiStatus.STABLE, PublicApiAccess()), versionDefinition("2.0", ApiStatus.STABLE, PublicApiAccess()))
  val mixedAccessVersions = List(versionDefinition("1.0", ApiStatus.STABLE, PublicApiAccess()), versionDefinition("1.0", ApiStatus.STABLE, PrivateApiAccess()), versionDefinition("1.0", ApiStatus.RETIRED, PrivateApiAccess()))
  val api1AllPublic = newDefinition(allPublicVersions)
  val api1mixedAccess = newDefinition(mixedAccessVersions)


  "ApiAccessRules" when {

    "allVersionsArePublicAccess" should {
      "return true when an apis has only public accessTypes" in {
        allVersionsArePublicAccess(api1AllPublic) shouldBe true
      }
      "return false when an apis has mix of public  and private accessTypes" in {
        allVersionsArePublicAccess(api1mixedAccess) shouldBe false
      }

    }

    "filterOutRetiredApis" should {
      "not filter out api with only one version retired"  in {
        val testData = List(api1AllPublic, api1mixedAccess, api1AllPublic.copy(serviceName = "newName"))
        val filteredList = CombinedApiDataHelper.filterOutRetiredApis(testData)
        filteredList should contain only (testData : _*)
      }
    }
  }
}
