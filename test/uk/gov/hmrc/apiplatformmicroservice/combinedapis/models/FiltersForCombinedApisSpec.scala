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

package uk.gov.hmrc.apiplatformmicroservice.combinedapis.models

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.services.FiltersForCombinedApis
import uk.gov.hmrc.apiplatformmicroservice.combinedapis.utils.CombinedApiDataHelper
import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec

class FiltersForCombinedApisSpec extends AsyncHmrcSpec with FiltersForCombinedApis {

  val endpoint1 = Endpoint("endpoint1", "/some/endpoint1", HttpMethod.POST, AuthType.USER)

  def versionDefinition(version: String, status: ApiStatus, apiAccess: ApiAccess) = {
    ApiVersion(ApiVersionNbr(version), status, apiAccess, List(endpoint1))
  }

  def newDefinition(versions: List[ApiVersion]) = {
    ApiDefinition(ServiceName("test1ServiceName"), "someUrl", "test1Name", "test1Desc", ApiContext("som/context/here"), versions, requiresTrust = false, isTestSupport = false, None, List.empty)
  }

  val allPublicVersions: List[ApiVersion] =
    List(versionDefinition("1.0", ApiStatus.STABLE, ApiAccess.PUBLIC), versionDefinition("2.0", ApiStatus.STABLE, ApiAccess.PUBLIC))

  val mixedAccessVersions = List(
    versionDefinition("1.0", ApiStatus.STABLE, ApiAccess.PUBLIC),
    versionDefinition("1.0", ApiStatus.STABLE, ApiAccess.Private(false)),
    versionDefinition("1.0", ApiStatus.RETIRED, ApiAccess.Private(false))
  )
  val api1AllPublic       = newDefinition(allPublicVersions)
  val api1mixedAccess     = newDefinition(mixedAccessVersions)

  "FiltersForCombinedApis" when {

    "allVersionsArePublicAccess" should {
      "return true when an apis has only public accessTypes" in {
        allVersionsArePublicAccess(api1AllPublic) shouldBe true
      }
      "return false when an apis has mix of public  and private accessTypes" in {
        allVersionsArePublicAccess(api1mixedAccess) shouldBe false
      }

    }

    "filterOutRetiredApis" should {
      "not filter out api with only one version retired" in {
        val testData     = List(api1AllPublic, api1mixedAccess, api1AllPublic.copy(serviceName = ServiceName("newName")))
        val filteredList = CombinedApiDataHelper.filterOutRetiredApis(testData)
        filteredList should contain.only(
          api1AllPublic,
          api1AllPublic.copy(serviceName = ServiceName("newName")),
          api1mixedAccess.copy(versions = List(versionDefinition("1.0", ApiStatus.STABLE, ApiAccess.PUBLIC), versionDefinition("1.0", ApiStatus.STABLE, ApiAccess.Private(false))))
        )
      }

      "filter out api with only retired versions" in {
        val apiWithOnlyRetiredVersions = api1mixedAccess.copy(versions =
          List(
            versionDefinition("1.0", ApiStatus.RETIRED, ApiAccess.PUBLIC),
            versionDefinition("2.0", ApiStatus.RETIRED, ApiAccess.PUBLIC),
            versionDefinition("3.0", ApiStatus.RETIRED, ApiAccess.Private(false))
          )
        )
        val testData                   = List(api1AllPublic, api1AllPublic.copy(serviceName = ServiceName("newName")), apiWithOnlyRetiredVersions)
        val filteredList               = CombinedApiDataHelper.filterOutRetiredApis(testData)
        filteredList should contain.only(api1AllPublic, api1AllPublic.copy(serviceName =ServiceName("newName")))
      }
    }

    "allVersionsArePublicAccess" should {
      val endpoint            = Endpoint("endpoint1", "uri/pattern", HttpMethod.GET, AuthType.USER, ResourceThrottlingTier.UNLIMITED, None, List.empty)
      val allPublicApiVersion = ExtendedAPIVersion(
        ApiVersionNbr("1.0"),
        ApiStatus.STABLE,
        List(endpoint),
        Some(ApiAvailability(true, ApiAccess.PUBLIC, true, true)),
        Some(ApiAvailability(true, ApiAccess.PUBLIC, true, true))
      )

      val mixedApiVersions = ExtendedAPIVersion(
        ApiVersionNbr("1.0"),
        ApiStatus.STABLE,
        List(endpoint),
        Some(ApiAvailability(true, ApiAccess.Private(false), true, true)),
        Some(ApiAvailability(true, ApiAccess.PUBLIC, true, true))
      )

      val mixedApiVersionsWithNone =
        ExtendedAPIVersion(ApiVersionNbr("1.0"), ApiStatus.STABLE, List(endpoint), Some(ApiAvailability(true, ApiAccess.Private(false), true, true)), None)

      "return true when all versions are public access" in {

        val extendedApiDefinition =
          ExtendedAPIDefinition(
            "serviceName",
            "url",
            "name",
            "desc",
            ApiContext("/some/context"),
            false,
            false,
            List(allPublicApiVersion, allPublicApiVersion.copy(version = ApiVersionNbr("2.0"))),
            List(ApiCategory.OTHER),
            None
          )

        allVersionsArePublicAccess(extendedApiDefinition) shouldBe true
      }

      "return false when some versions are private access" in {
        val extendedApiDefinition =
          ExtendedAPIDefinition(
            "serviceName",
            "basurl",
            "name",
            "desc",
            ApiContext("/some/context"),
            false,
            false,
            List(mixedApiVersions, allPublicApiVersion.copy(version = ApiVersionNbr("2.0"))),
            List(ApiCategory.OTHER),
            None
          )

        allVersionsArePublicAccess(extendedApiDefinition) shouldBe false
      }

      "return false when some versions have none" in {
        val extendedApiDefinition =
          ExtendedAPIDefinition(
            serviceName = "serviceName",
            serviceBaseUrl = "baseUrl",
            "name",
            "desc",
            ApiContext("/some/context"),
            false,
            false,
            List(mixedApiVersionsWithNone, allPublicApiVersion.copy(version = ApiVersionNbr("2.0"))),
            List(ApiCategory.AGENTS),
            None
          )

        allVersionsArePublicAccess(extendedApiDefinition) shouldBe false
      }
    }
  }
}
