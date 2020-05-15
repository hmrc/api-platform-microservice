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

import uk.gov.hmrc.apiplatformmicroservice.apidefinition.mocks.ApiDefinitionConnectorModule
import uk.gov.hmrc.apiplatformmicroservice.util.AsyncHmrcSpec
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.APIStatus.{RETIRED, STABLE}
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiDefinitionTestDataHelper
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks.ApplicationIdsForCollaboratorFetcherModule
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class ApiDefinitionsForCollaboratorFetcherSpec extends AsyncHmrcSpec with ApiDefinitionTestDataHelper {

  trait Setup extends ApiDefinitionConnectorModule with ApplicationIdsForCollaboratorFetcherModule {
    implicit val headerCarrier = HeaderCarrier()
    val email = "joebloggs@example.com"
    val applicationId = "app-1"
    val helloApiDefinition = apiDefinition("hello-api")
    val apiWithNoAccess = apiDefinition("api-with-no-access", Seq(apiVersion(access = None)))
    val requiresTrustApi = apiDefinition("requires-trust-api").doesRequireTrust
    val apiWithOnlyRetiredVersions = apiDefinition("api-with-retired-versions", Seq(apiVersion("1.0", RETIRED),
                                                                                       apiVersion("2.0", RETIRED)))

    val apiWithRetiredVersions = apiDefinition("api-with-retired-versions", Seq(apiVersion("1.0", RETIRED),
                                                                                       apiVersion("2.0", STABLE)))
    val apiWithPublicAndPrivateVersions = apiDefinition("api-with-public-and-private-versions",
      Seq(apiVersion("1.0", access = Some(apiAccess().asPrivate)), apiVersion("2.0", access = Some(apiAccess()))))

    val apiWithOnlyPrivateVersions = apiDefinition("api-with-private-versions",
      Seq(apiVersion("1.0", access = Some(apiAccess().asPrivate)), apiVersion("2.0", access = Some(apiAccess().asPrivate))))

    val apiWithPrivateTrials = apiDefinition("api-with-trials", Seq(apiVersion("1.0", access = Some(apiAccess().asPrivate.asTrial))))
    val apiWithWhitelisting = apiDefinition("api-with-whitelisting",
      Seq(apiVersion("1.0", access = Some(apiAccess().asPrivate.withWhitelistedAppIds(applicationId)))))
    val underTest = new ApiDefinitionsForCollaboratorFetcher(ApiDefinitionConnectorMock.aMock, ApplicationIdsForCollaboratorFetcherMock.aMock)
  }

  "ApiDefinitionsForCollaboratorFetcher" should {
    "return the public APIs" in new Setup {
      ApiDefinitionConnectorMock.FetchAllApiDefinitions.willReturnApiDefinitions(apiWithNoAccess)
      ApplicationIdsForCollaboratorFetcherMock.FetchAllApplicationIds.willReturnApplicationIds(Seq.empty: _*)

      val result = await(underTest(email))

      result mustBe Seq(apiWithNoAccess)
    }

    "return APIs with no access" in new Setup {
      ApiDefinitionConnectorMock.FetchAllApiDefinitions.willReturnApiDefinitions(helloApiDefinition)
      ApplicationIdsForCollaboratorFetcherMock.FetchAllApplicationIds.willReturnApplicationIds(Seq.empty: _*)

      val result = await(underTest(email))

      result mustBe Seq(helloApiDefinition)
    }

    "filter out an api that requires trust" in new Setup {
      ApiDefinitionConnectorMock.FetchAllApiDefinitions.willReturnApiDefinitions(helloApiDefinition, requiresTrustApi)
      ApplicationIdsForCollaboratorFetcherMock.FetchAllApplicationIds.willReturnApplicationIds(Seq.empty: _*)

      val result = await(underTest(email))

      result mustBe Seq(helloApiDefinition)
    }

    "filter out an api that only has retired versions" in new Setup {
      ApiDefinitionConnectorMock.FetchAllApiDefinitions.willReturnApiDefinitions(apiWithRetiredVersions, apiWithOnlyRetiredVersions)
      ApplicationIdsForCollaboratorFetcherMock.FetchAllApplicationIds.willReturnApplicationIds(Seq.empty: _*)

      val result = await(underTest(email))

      result mustBe Seq(apiWithRetiredVersions)
    }

    "filter out private versions for an api" in new Setup {
      ApiDefinitionConnectorMock.FetchAllApiDefinitions.willReturnApiDefinitions(apiWithPublicAndPrivateVersions)
      ApplicationIdsForCollaboratorFetcherMock.FetchAllApplicationIds.willReturnApplicationIds(Seq.empty: _*)

      val result = await(underTest(email))

      result.head.versions mustBe Seq(apiVersion("2.0", access = Some(apiAccess())))
    }

    "filter out an api if it only has private versions" in new Setup {
      ApiDefinitionConnectorMock.FetchAllApiDefinitions.willReturnApiDefinitions(apiWithOnlyPrivateVersions)
      ApplicationIdsForCollaboratorFetcherMock.FetchAllApplicationIds.willReturnApplicationIds(Seq.empty: _*)

      val result = await(underTest(email))

      result mustBe Seq.empty
    }

    "return api if it's private but with trials" in new Setup {
      ApiDefinitionConnectorMock.FetchAllApiDefinitions.willReturnApiDefinitions(apiWithPrivateTrials)
      ApplicationIdsForCollaboratorFetcherMock.FetchAllApplicationIds.willReturnApplicationIds(Seq.empty: _*)

      val result = await(underTest(email))

      result mustBe Seq(apiWithPrivateTrials)
    }

    "return api if it's private but the user has a whitelisted application" in new Setup {
      ApiDefinitionConnectorMock.FetchAllApiDefinitions.willReturnApiDefinitions(apiWithWhitelisting)
      ApplicationIdsForCollaboratorFetcherMock.FetchAllApplicationIds.willReturnApplicationIds(applicationId)

      val result = await(underTest(email))

      result mustBe Seq(apiWithWhitelisting)
    }
  }
}
