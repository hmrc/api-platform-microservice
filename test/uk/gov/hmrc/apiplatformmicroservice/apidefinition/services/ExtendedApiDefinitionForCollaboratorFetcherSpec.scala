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

import uk.gov.hmrc.apiplatformmicroservice.apidefinition.mocks.ApiDefinitionServiceModule
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.APIStatus.{BETA, RETIRED, STABLE}
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.{APIAvailability, ApiDefinitionTestDataHelper, ApiVersion, PrivateApiAccess, PublicApiAccess}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks.ApplicationIdsForCollaboratorFetcherModule
import uk.gov.hmrc.apiplatformmicroservice.util.AsyncHmrcSpec
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class ExtendedApiDefinitionForCollaboratorFetcherSpec extends AsyncHmrcSpec with ApiDefinitionTestDataHelper {

  private val versionOne = ApiVersion("1.0")
  private val versionTwo = ApiVersion("2.0")

  trait Setup extends ApiDefinitionServiceModule with ApplicationIdsForCollaboratorFetcherModule {
    implicit val headerCarrier = HeaderCarrier()
    val email = Some("joebloggs@example.com")
    val applicationId = "app-1"
    val helloApiDefinition = apiDefinition("hello-api")
    val requiresTrustApi = apiDefinition("requires-trust-api").doesRequireTrust
    val apiWithOnlyRetiredVersions = apiDefinition("api-with-retired-versions", apiVersion(versionOne, RETIRED), apiVersion(versionTwo, RETIRED))

    val apiWithRetiredVersions = apiDefinition("api-with-retired-versions", apiVersion(versionOne, RETIRED), apiVersion(versionTwo, STABLE))

    val apiWithPublicAndPrivateVersions =
      apiDefinition("api-with-public-and-private-versions", apiVersion(versionOne, access = PrivateApiAccess()), apiVersion(versionTwo, access = apiAccess()))

    val apiWithWhitelisting = apiDefinition("api-with-whitelisting", apiVersion(versionOne, access = PrivateApiAccess().withWhitelistedAppIds(applicationId)))

    val underTest = new ExtendedApiDefinitionForCollaboratorFetcher(
      PrincipalApiDefinitionServiceMock.aMock,
      SubordinateApiDefinitionServiceMock.aMock,
      ApplicationIdsForCollaboratorFetcherMock.aMock
    )

    val publicApiAvailability = APIAvailability(false, PublicApiAccess(), false, true)
    val privateApiAvailability = APIAvailability(false, PrivateApiAccess(List(), false), false, false)
  }

  "ExtendedApiDefinitionForCollaboratorFetcher" should {
    "return an extended api with only production availability when api only in principal" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchDefinition.willReturnApiDefinition(helloApiDefinition)
      SubordinateApiDefinitionServiceMock.FetchDefinition.willReturnNoApiDefinition()

      val Some(result) = await(underTest.fetch(helloApiDefinition.serviceName, None))

      result.versions.head.productionAvailability mustBe Some(publicApiAvailability)
      result.versions.head.sandboxAvailability mustBe None
    }

    "return an extended api with only sandbox availability when api only in subordinate" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchDefinition.willReturnNoApiDefinition()
      SubordinateApiDefinitionServiceMock.FetchDefinition.willReturnApiDefinition(helloApiDefinition)

      val Some(result) = await(underTest.fetch(helloApiDefinition.serviceName, None))

      result.versions.head.productionAvailability mustBe None
      result.versions.head.sandboxAvailability mustBe Some(publicApiAvailability)
    }

    "return an extended api with production and sandbox availability when api in both environments" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchDefinition.willReturnApiDefinition(helloApiDefinition)
      SubordinateApiDefinitionServiceMock.FetchDefinition.willReturnApiDefinition(helloApiDefinition)

      val Some(result) = await(underTest.fetch(helloApiDefinition.serviceName, None))

      result.versions must have size 1
      result.versions.head.sandboxAvailability mustBe Some(publicApiAvailability)
      result.versions.head.productionAvailability mustBe Some(publicApiAvailability)
    }

    "prefer subordinate API when it exists in both environments" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchDefinition.willReturnApiDefinition(helloApiDefinition.withName("hello-principal"))
      SubordinateApiDefinitionServiceMock.FetchDefinition.willReturnApiDefinition(helloApiDefinition.withName("hello-subordinate"))

      val Some(result) = await(underTest.fetch(helloApiDefinition.serviceName, None))

      result.name mustBe "hello-subordinate"
    }

    "prefer subordinate version when it exists in both environments" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchDefinition.willReturnApiDefinition(helloApiDefinition.withVersions(apiVersion(versionOne, BETA)))
      SubordinateApiDefinitionServiceMock.FetchDefinition.willReturnApiDefinition(helloApiDefinition.withVersions(apiVersion(versionOne, STABLE)))

      val Some(result) = await(underTest.fetch(helloApiDefinition.serviceName, None))

      result.versions must have size 1
      result.versions.head.status mustBe STABLE
    }

    "return none when api doesn't exist in any environments" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchDefinition.willReturnNoApiDefinition()
      SubordinateApiDefinitionServiceMock.FetchDefinition.willReturnNoApiDefinition()

      val result = await(underTest.fetch(helloApiDefinition.serviceName, None))

      result mustBe None
    }

    "return none when apis requires trust" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchDefinition.willReturnApiDefinition(requiresTrustApi)
      SubordinateApiDefinitionServiceMock.FetchDefinition.willReturnApiDefinition(requiresTrustApi)

      val result = await(underTest.fetch(helloApiDefinition.serviceName, None))

      result mustBe None
    }

    "filter out retired versions" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchDefinition.willReturnNoApiDefinition()
      SubordinateApiDefinitionServiceMock.FetchDefinition.willReturnApiDefinition(apiWithRetiredVersions)

      val Some(result) = await(underTest.fetch(helloApiDefinition.serviceName, None))

      result.versions must have size 1
      result.versions.head.status mustBe STABLE
    }

    "return none if all verions are retired" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchDefinition.willReturnNoApiDefinition()
      SubordinateApiDefinitionServiceMock.FetchDefinition.willReturnApiDefinition(apiWithOnlyRetiredVersions)

      val result = await(underTest.fetch(helloApiDefinition.serviceName, None))

      result mustBe None
    }

    "return public and private availability for api public and private versions " in new Setup {
      PrincipalApiDefinitionServiceMock.FetchDefinition.willReturnNoApiDefinition()
      SubordinateApiDefinitionServiceMock.FetchDefinition.willReturnApiDefinition(apiWithPublicAndPrivateVersions)

      val Some(result) = await(underTest.fetch(helloApiDefinition.serviceName, None))

      result.versions.map(_.sandboxAvailability) must contain only (Some(privateApiAvailability), Some(publicApiAvailability))
      result.versions.map(_.productionAvailability) must contain only None
    }

    "return true when application ids are matching" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchDefinition.willReturnNoApiDefinition()
      SubordinateApiDefinitionServiceMock.FetchDefinition.willReturnApiDefinition(apiWithWhitelisting)
      ApplicationIdsForCollaboratorFetcherMock.FetchAllApplicationIds.willReturnApplicationIds(applicationId)

      val Some(result) = await(underTest.fetch(helloApiDefinition.serviceName, email))

      result.versions.head.sandboxAvailability.map(_.authorised) mustBe Some(true)
    }

    "return false when applications ids are not matching" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchDefinition.willReturnNoApiDefinition()
      SubordinateApiDefinitionServiceMock.FetchDefinition.willReturnApiDefinition(apiWithWhitelisting)
      ApplicationIdsForCollaboratorFetcherMock.FetchAllApplicationIds.willReturnApplicationIds("NonMatchingID")

      val Some(result) = await(underTest.fetch(helloApiDefinition.serviceName, email))

      result.versions.head.sandboxAvailability.map(_.authorised) mustBe Some(false)
    }

  }
}
