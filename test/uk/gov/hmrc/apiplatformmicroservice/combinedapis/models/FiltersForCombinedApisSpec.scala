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

import cats.data.NonEmptyList

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models._
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.services.FiltersForCombinedApis
import uk.gov.hmrc.apiplatformmicroservice.combinedapis.utils.CombinedApiDataHelper
import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec

class FiltersForCombinedApisSpec extends AsyncHmrcSpec with FiltersForCombinedApis {

  val endpoint1 = Endpoint("endpoint1", "/some/endpoint1", HttpMethod.POST, AuthType.USER)

  def versionDefinition(version: String, status: ApiStatus, apiAccess: ApiAccess) = {
    ApiVersionDefinition(ApiVersionNbr(version), status, apiAccess, List(endpoint1))
  }

  def newDefinition(versions: List[ApiVersionDefinition]) = {
    ApiDefinition("test1ServiceName", "someUrl", "test1Name", "test1Desc", ApiContext("som/context/here"), requiresTrust = false, isTestSupport = false, versions)
  }

  val allPublicVersions: List[ApiVersionDefinition] =
    List(versionDefinition("1.0", ApiStatus.STABLE, ApiAccess.PUBLIC), versionDefinition("2.0", ApiStatus.STABLE, ApiAccess.PUBLIC))

  val mixedAccessVersions = List(
    versionDefinition("1.0", ApiStatus.STABLE, ApiAccess.PUBLIC),
    versionDefinition("1.0", ApiStatus.STABLE, ApiAccess.Private(Nil, false)),
    versionDefinition("1.0", ApiStatus.RETIRED, ApiAccess.Private(Nil, false))
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
        val testData     = List(api1AllPublic, api1mixedAccess, api1AllPublic.copy(serviceName = "newName"))
        val filteredList = CombinedApiDataHelper.filterOutRetiredApis(testData)
        filteredList should contain only (api1AllPublic, api1AllPublic.copy(serviceName = "newName"),
        api1mixedAccess.copy(versions = List(versionDefinition("1.0", ApiStatus.STABLE, ApiAccess.PUBLIC), versionDefinition("1.0", ApiStatus.STABLE, ApiAccess.Private(Nil, false)))))
      }

      "filter out api with only retired versions" in {
        val apiWithOnlyRetiredVersions = api1mixedAccess.copy(versions =
          List(
            versionDefinition("1.0", ApiStatus.RETIRED, ApiAccess.PUBLIC),
            versionDefinition("2.0", ApiStatus.RETIRED, ApiAccess.PUBLIC),
            versionDefinition("3.0", ApiStatus.RETIRED, ApiAccess.Private(Nil, false))
          )
        )
        val testData                   = List(api1AllPublic, api1AllPublic.copy(serviceName = "newName"), apiWithOnlyRetiredVersions)
        val filteredList               = CombinedApiDataHelper.filterOutRetiredApis(testData)
        filteredList should contain only (api1AllPublic, api1AllPublic.copy(serviceName = "newName"))
      }
    }

    "allVersionsArePublicAccess" should {
      val endpoint            = Endpoint("endpoint1", "uri/pattern", HttpMethod.GET, AuthType.USER, ResourceThrottlingTier.UNLIMITED, None, List.empty)
      val allPublicApiVersion = ExtendedApiVersion(
        ApiVersionNbr("1.0"),
        ApiStatus.STABLE,
        List(endpoint),
        Some(ApiAvailability(true, ApiAccess.PUBLIC, true, true)),
        Some(ApiAvailability(true, ApiAccess.PUBLIC, true, true))
      )

      val mixedApiVersions = ExtendedApiVersion(
        ApiVersionNbr("1.0"),
        ApiStatus.STABLE,
        List(endpoint),
        Some(ApiAvailability(true, ApiAccess.Private(Nil, false), true, true)),
        Some(ApiAvailability(true, ApiAccess.PUBLIC, true, true))
      )

      val mixedApiVersionsWithNone =
        ExtendedApiVersion(ApiVersionNbr("1.0"), ApiStatus.STABLE, List(endpoint), Some(ApiAvailability(true, ApiAccess.Private(Nil, false), true, true)), None)

      "return true when all versions are public access" in {

        val extendedApiDefinition =
          ExtendedApiDefinition(
            "serviceName",
            "name",
            "desc",
            ApiContext("/some/context"),
            false,
            false,
            List(allPublicApiVersion, allPublicApiVersion.copy(version = ApiVersionNbr("2.0")))
          )

        allVersionsArePublicAccess(extendedApiDefinition) shouldBe true
      }

      "return false when some versions are private access" in {
        val extendedApiDefinition =
          ExtendedApiDefinition("serviceName", "name", "desc", ApiContext("/some/context"), false, false, List(mixedApiVersions, allPublicApiVersion.copy(version = ApiVersionNbr("2.0"))))

        allVersionsArePublicAccess(extendedApiDefinition) shouldBe false
      }

      "return false when some versions have none" in {
        val extendedApiDefinition =
          ExtendedApiDefinition(
            "serviceName",
            "name",
            "desc",
            ApiContext("/some/context"),
            false,
            false,
            List(mixedApiVersionsWithNone, allPublicApiVersion.copy(version = ApiVersionNbr("2.0")))
          )

        allVersionsArePublicAccess(extendedApiDefinition) shouldBe false
      }
    }
  }
}
