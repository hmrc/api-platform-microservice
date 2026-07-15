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

import uk.gov.hmrc.apiplatform.modules.common.domain.models.*
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.*
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.mocks.ApiDefinitionServiceModule
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiDefinitionTestDataHelper
import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks.SubscriptionsForCollaboratorFetcherModule

class ApiDefinitionsForCollaboratorFetcherSpec extends AsyncHmrcSpec with ApiDefinitionTestDataHelper {

  private val versionOne = ApiVersionNbr("1.0")
  private val versionTwo = ApiVersionNbr("2.0")

  trait Setup extends ApiDefinitionServiceModule with SubscriptionsForCollaboratorFetcherModule {
    implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
    val userId                                = Some(UserId.random)
    val applicationId                         = ApplicationId.random
    val helloApiDefinition                    = apiDefinition("hello-api")
    val apiWithOnlyRetiredVersions            = apiDefinition("api-with-retired-versions", apiVersion(versionOne, ApiStatus.Retired), apiVersion(versionTwo, ApiStatus.Retired))

    val apiWithRetiredVersions = apiDefinition("api-with-retired-versions", apiVersion(versionOne, ApiStatus.Retired), apiVersion(versionTwo, ApiStatus.Stable))

    val apiWithPublicAndInternalVersions =
      apiDefinition("api-with-public-and-internal-versions", apiVersion(versionOne, access = ApiAccessType.Internal), apiVersion(versionTwo, access = ApiAccessType.Public))

    val apiWithOnlyInternalVersions =
      apiDefinition("api-with-internal-versions", apiVersion(versionOne, access = ApiAccessType.Internal), apiVersion(versionTwo, access = ApiAccessType.Internal))

    val apiWithControlled = apiDefinition("api-with-controlled", apiVersion(versionOne, access = ApiAccessType.Controlled))

    val underTest =
      new ApiDefinitionsForCollaboratorFetcher(
        PrincipalApiDefinitionServiceMock.aMock,
        SubordinateApiDefinitionServiceMock.aMock,
        SubscriptionsForCollaboratorFetcherMock.aMock
      )
    SubordinateApiDefinitionServiceMock.FetchAllApiDefinitions.willReturnNones()
    SubscriptionsForCollaboratorFetcherMock.willReturnSubscriptions()
  }

  "ApiDefinitionsForCollaboratorFetcher" should {
    "return the public APIs" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchAllApiDefinitions.willReturn(helloApiDefinition)

      val result = await(underTest.fetch(userId))

      result should contain only (helloApiDefinition)
    }

    "prefer subordinate API when it is present in both environments" in new Setup {
      val principalHelloApi   = helloApiDefinition.withName("hello-principal")
      val subordinateHelloApi = helloApiDefinition.withName("hello-subordinate")
      PrincipalApiDefinitionServiceMock.FetchAllApiDefinitions.willReturn(principalHelloApi)
      SubordinateApiDefinitionServiceMock.FetchAllApiDefinitions.willReturn(subordinateHelloApi)

      val result = await(underTest.fetch(userId))

      result should contain only (subordinateHelloApi)
    }

    "filter out an api that only has retired versions" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchAllApiDefinitions.willReturn(apiWithRetiredVersions, apiWithOnlyRetiredVersions)

      val result = await(underTest.fetch(userId))

      result.map(_.name) should contain only (apiWithRetiredVersions.name)
      result.head.versions.keySet should contain only (versionTwo)
    }

    "filter out non-public versions for an api" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchAllApiDefinitions.willReturn(apiWithPublicAndInternalVersions)

      val result = await(underTest.fetch(userId))

      result.head.versions.values should contain only (apiVersion(versionTwo, access = ApiAccessType.Public))
    }

    "filter out non-public versions for an api if no email provided" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchAllApiDefinitions.willReturn(apiWithPublicAndInternalVersions)

      val result = await(underTest.fetch(None))

      result.head.versions.values should contain only (apiVersion(versionTwo, access = ApiAccessType.Public))
    }

    "filter out an api if it only has non-public versions" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchAllApiDefinitions.willReturn(apiWithOnlyInternalVersions)

      val result = await(underTest.fetch(userId))

      result shouldBe empty
    }

    "return api if it's controlled" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchAllApiDefinitions.willReturn(apiWithControlled)

      val result = await(underTest.fetch(userId))

      result should contain only (apiWithControlled)
    }
  }
}
