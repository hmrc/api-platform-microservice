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
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.APIStatus.{RETIRED, STABLE}
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.{ApiDefinitionTestDataHelper, PrivateApiAccess}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks.ApplicationIdsForCollaboratorFetcherModule
import uk.gov.hmrc.apiplatformmicroservice.util.AsyncHmrcSpec
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class ApiDefinitionsForCollaboratorFetcherSpec extends AsyncHmrcSpec with ApiDefinitionTestDataHelper {

  trait Setup extends ApiDefinitionServiceModule with ApplicationIdsForCollaboratorFetcherModule {
    implicit val headerCarrier = HeaderCarrier()
    val email = Some("joebloggs@example.com")
    val applicationId = "app-1"
    val helloApiDefinition = apiDefinition("hello-api")
    val requiresTrustApi = apiDefinition("requires-trust-api").doesRequireTrust
    val apiWithOnlyRetiredVersions = apiDefinition("api-with-retired-versions", Seq(apiVersion("1.0", RETIRED),
                                                                                       apiVersion("2.0", RETIRED)))

    val apiWithRetiredVersions = apiDefinition("api-with-retired-versions", Seq(apiVersion("1.0", RETIRED),
                                                                                       apiVersion("2.0", STABLE)))
    val apiWithPublicAndPrivateVersions = apiDefinition("api-with-public-and-private-versions",
      Seq(apiVersion("1.0", access = PrivateApiAccess()), apiVersion("2.0", access = apiAccess())))

    val apiWithOnlyPrivateVersions = apiDefinition("api-with-private-versions",
      Seq(apiVersion("1.0", access = PrivateApiAccess()), apiVersion("2.0", access = PrivateApiAccess())))

    val apiWithPrivateTrials = apiDefinition("api-with-trials", Seq(apiVersion("1.0", access = PrivateApiAccess().asTrial)))
    val apiWithWhitelisting = apiDefinition("api-with-whitelisting",
      Seq(apiVersion("1.0", access = PrivateApiAccess().withWhitelistedAppIds(applicationId))))
    val underTest = new ApiDefinitionsForCollaboratorFetcher(PrincipalApiDefinitionServiceMock.aMock,
      SubordinateApiDefinitionServiceMock.aMock ,ApplicationIdsForCollaboratorFetcherMock.aMock)
    SubordinateApiDefinitionServiceMock.FetchAllDefinitions.willReturnNoApiDefinitions()
  }

  "ApiDefinitionsForCollaboratorFetcher" should {
    "return the public APIs" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchAllDefinitions.willReturnApiDefinitions(helloApiDefinition)
      ApplicationIdsForCollaboratorFetcherMock.FetchAllApplicationIds.willReturnApplicationIds(Seq.empty: _*)

      val result = await(underTest.fetch(email))

      result mustBe Seq(helloApiDefinition)
    }

    "prefer subordinate API when it is present in both environments" in new Setup {
      val principalHelloApi = helloApiDefinition.withName("hello-principal")
      val subordinateHelloApi = helloApiDefinition.withName("hello-subordinate")
      PrincipalApiDefinitionServiceMock.FetchAllDefinitions.willReturnApiDefinitions(principalHelloApi)
      SubordinateApiDefinitionServiceMock.FetchAllDefinitions.willReturnApiDefinitions(subordinateHelloApi)
      ApplicationIdsForCollaboratorFetcherMock.FetchAllApplicationIds.willReturnApplicationIds(Seq.empty: _*)

      val result = await(underTest.fetch(email))

      result mustBe Seq(subordinateHelloApi)
    }

    "filter out an api that requires trust" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchAllDefinitions.willReturnApiDefinitions(helloApiDefinition, requiresTrustApi)
      ApplicationIdsForCollaboratorFetcherMock.FetchAllApplicationIds.willReturnApplicationIds(Seq.empty: _*)

      val result = await(underTest.fetch(email))

      result mustBe Seq(helloApiDefinition)
    }

    "filter out an api that only has retired versions" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchAllDefinitions.willReturnApiDefinitions(apiWithRetiredVersions, apiWithOnlyRetiredVersions)
      ApplicationIdsForCollaboratorFetcherMock.FetchAllApplicationIds.willReturnApplicationIds(Seq.empty: _*)

      val result = await(underTest.fetch(email))

      result.map(_.name) mustBe Seq(apiWithRetiredVersions.name)
      result.head.versions.map(_.version) mustBe Seq("2.0")
    }

    "filter out private versions for an api" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchAllDefinitions.willReturnApiDefinitions(apiWithPublicAndPrivateVersions)
      ApplicationIdsForCollaboratorFetcherMock.FetchAllApplicationIds.willReturnApplicationIds(Seq.empty: _*)

      val result = await(underTest.fetch(email))

      result.head.versions mustBe Seq(apiVersion("2.0", access = apiAccess()))
    }

    "filter out private versions for an api if no email provided" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchAllDefinitions.willReturnApiDefinitions(apiWithPublicAndPrivateVersions)

      val result = await(underTest.fetch(None))

      result.head.versions mustBe Seq(apiVersion("2.0", access = apiAccess()))
    }

    "filter out an api if it only has private versions" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchAllDefinitions.willReturnApiDefinitions(apiWithOnlyPrivateVersions)
      ApplicationIdsForCollaboratorFetcherMock.FetchAllApplicationIds.willReturnApplicationIds(Seq.empty: _*)

      val result = await(underTest.fetch(email))

      result mustBe Seq.empty
    }

    "return api if it's private but with trials" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchAllDefinitions.willReturnApiDefinitions(apiWithPrivateTrials)
      ApplicationIdsForCollaboratorFetcherMock.FetchAllApplicationIds.willReturnApplicationIds(Seq.empty: _*)

      val result = await(underTest.fetch(email))

      result mustBe Seq(apiWithPrivateTrials)
    }

    "return api if it's private but the user has a whitelisted application" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchAllDefinitions.willReturnApiDefinitions(apiWithWhitelisting)
      ApplicationIdsForCollaboratorFetcherMock.FetchAllApplicationIds.willReturnApplicationIds(applicationId)

      val result = await(underTest.fetch(email))

      result mustBe Seq(apiWithWhitelisting)
    }
  }
}
