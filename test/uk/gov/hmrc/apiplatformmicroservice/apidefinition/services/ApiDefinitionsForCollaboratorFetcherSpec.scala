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

import scala.concurrent.ExecutionContext.Implicits.global

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatformmicroservice.apidefinition.mocks.ApiDefinitionServiceModule
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiStatus.{RETIRED, STABLE}
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.{ApiDefinitionTestDataHelper, PrivateApiAccess}
import  uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.developers.domain.models.UserId
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.ApplicationId
import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks.{ApplicationIdsForCollaboratorFetcherModule, SubscriptionsForCollaboratorFetcherModule}

class ApiDefinitionsForCollaboratorFetcherSpec extends AsyncHmrcSpec with ApiDefinitionTestDataHelper {

  private val versionOne = ApiVersion("1.0")
  private val versionTwo = ApiVersion("2.0")

  trait Setup extends ApiDefinitionServiceModule with ApplicationIdsForCollaboratorFetcherModule with SubscriptionsForCollaboratorFetcherModule {
    implicit val headerCarrier     = HeaderCarrier()
    val userId                     = Some(UserId.random)
    val applicationId              = ApplicationId.random
    val helloApiDefinition         = apiDefinition("hello-api")
    val requiresTrustApi           = apiDefinition("requires-trust-api").doesRequireTrust
    val apiWithOnlyRetiredVersions = apiDefinition("api-with-retired-versions", apiVersion(versionOne, RETIRED), apiVersion(versionTwo, RETIRED))

    val apiWithRetiredVersions = apiDefinition("api-with-retired-versions", apiVersion(versionOne, RETIRED), apiVersion(versionTwo, STABLE))

    val apiWithPublicAndPrivateVersions =
      apiDefinition("api-with-public-and-private-versions", apiVersion(versionOne, access = PrivateApiAccess()), apiVersion(versionTwo, access = apiAccess()))

    val apiWithOnlyPrivateVersions =
      apiDefinition("api-with-private-versions", apiVersion(versionOne, access = PrivateApiAccess()), apiVersion(versionTwo, access = PrivateApiAccess()))

    val apiWithPrivateTrials = apiDefinition("api-with-trials", apiVersion(versionOne, access = PrivateApiAccess().asTrial))
    val apiWithAllowlisting  = apiDefinition("api-with-allowlisting", apiVersion(versionOne, access = PrivateApiAccess().withAllowlistedAppIds(applicationId)))

    val underTest =
      new ApiDefinitionsForCollaboratorFetcher(
        PrincipalApiDefinitionServiceMock.aMock,
        SubordinateApiDefinitionServiceMock.aMock,
        ApplicationIdsForCollaboratorFetcherMock.aMock,
        SubscriptionsForCollaboratorFetcherMock.aMock
      )
    SubordinateApiDefinitionServiceMock.FetchAllApiDefinitions.willReturnNones()
    SubscriptionsForCollaboratorFetcherMock.willReturnSubscriptions()
  }

  "ApiDefinitionsForCollaboratorFetcher" should {
    "return the public APIs" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchAllApiDefinitions.willReturn(helloApiDefinition)
      ApplicationIdsForCollaboratorFetcherMock.FetchAllApplicationIds.willReturnApplicationIds(List.empty: _*)

      val result = await(underTest.fetch(userId))

      result should contain only (helloApiDefinition)
    }

    "prefer subordinate API when it is present in both environments" in new Setup {
      val principalHelloApi   = helloApiDefinition.withName("hello-principal")
      val subordinateHelloApi = helloApiDefinition.withName("hello-subordinate")
      PrincipalApiDefinitionServiceMock.FetchAllApiDefinitions.willReturn(principalHelloApi)
      SubordinateApiDefinitionServiceMock.FetchAllApiDefinitions.willReturn(subordinateHelloApi)
      ApplicationIdsForCollaboratorFetcherMock.FetchAllApplicationIds.willReturnApplicationIds(List.empty: _*)

      val result = await(underTest.fetch(userId))

      result should contain only (subordinateHelloApi)
    }

    "filter out an api that requires trust" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchAllApiDefinitions.willReturn(helloApiDefinition, requiresTrustApi)
      ApplicationIdsForCollaboratorFetcherMock.FetchAllApplicationIds.willReturnApplicationIds(List.empty: _*)

      val result = await(underTest.fetch(userId))

      result should contain only (helloApiDefinition)
    }

    "filter out an api that only has retired versions" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchAllApiDefinitions.willReturn(apiWithRetiredVersions, apiWithOnlyRetiredVersions)
      ApplicationIdsForCollaboratorFetcherMock.FetchAllApplicationIds.willReturnApplicationIds(List.empty: _*)

      val result = await(underTest.fetch(userId))

      result.map(_.name) should contain only (apiWithRetiredVersions.name)
      result.head.versions.map(_.version) should contain only (versionTwo)
    }

    "filter out private versions for an api" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchAllApiDefinitions.willReturn(apiWithPublicAndPrivateVersions)
      ApplicationIdsForCollaboratorFetcherMock.FetchAllApplicationIds.willReturnApplicationIds(List.empty: _*)

      val result = await(underTest.fetch(userId))

      result.head.versions should contain only (apiVersion(versionTwo, access = apiAccess()))
    }

    "filter out private versions for an api if no email provided" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchAllApiDefinitions.willReturn(apiWithPublicAndPrivateVersions)

      val result = await(underTest.fetch(None))

      result.head.versions should contain only (apiVersion(versionTwo, access = apiAccess()))
    }

    "filter out an api if it only has private versions" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchAllApiDefinitions.willReturn(apiWithOnlyPrivateVersions)
      ApplicationIdsForCollaboratorFetcherMock.FetchAllApplicationIds.willReturnApplicationIds(List.empty: _*)

      val result = await(underTest.fetch(userId))

      result shouldBe empty
    }

    "return api if it's private but with trials" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchAllApiDefinitions.willReturn(apiWithPrivateTrials)
      ApplicationIdsForCollaboratorFetcherMock.FetchAllApplicationIds.willReturnApplicationIds(List.empty: _*)

      val result = await(underTest.fetch(userId))

      result should contain only (apiWithPrivateTrials)
    }

    "return api if it's private but the user has an allowlisted application" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchAllApiDefinitions.willReturn(apiWithAllowlisting)
      ApplicationIdsForCollaboratorFetcherMock.FetchAllApplicationIds.willReturnApplicationIds(applicationId)

      val result = await(underTest.fetch(userId))

      result should contain only (apiWithAllowlisting)
    }
  }
}
