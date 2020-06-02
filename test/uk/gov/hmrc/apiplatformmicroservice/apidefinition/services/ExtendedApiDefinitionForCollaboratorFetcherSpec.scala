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
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.{APIAvailability, ApiDefinitionTestDataHelper, ExtendedAPIDefinition, PrivateApiAccess, PublicApiAccess}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks.ApplicationIdsForCollaboratorFetcherModule
import uk.gov.hmrc.apiplatformmicroservice.util.AsyncHmrcSpec
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class ExtendedApiDefinitionForCollaboratorFetcherSpec extends AsyncHmrcSpec with ApiDefinitionTestDataHelper {

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
    val underTest = new ExtendedApiDefinitionForCollaboratorFetcher(PrincipalApiDefinitionServiceMock.aMock,
      SubordinateApiDefinitionServiceMock.aMock ,ApplicationIdsForCollaboratorFetcherMock.aMock)
    val extendedAPIDefinition = extendedApiDefinition("hello-api")
    val publicApiAvailability = APIAvailability(false, PublicApiAccess(), false, true)
  }

  "ExtendedApiDefinitionForCollaboratorFetcher" should {
    "return an extended api with only production availability when api only in principal" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchDefinition.willReturnApiDefinition(helloApiDefinition)
      SubordinateApiDefinitionServiceMock.FetchDefinition.willReturnNoApiDefinition()
      ApplicationIdsForCollaboratorFetcherMock.FetchAllApplicationIds

      val Some(result) = await(underTest(helloApiDefinition.serviceName, None))

      result.versions.head.productionAvailability mustBe Some(publicApiAvailability)
      result.versions.head.sandboxAvailability mustBe None
    }

    "return an extended api with only sandbox availability when api only in subordinate" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchDefinition.willReturnNoApiDefinition()
      SubordinateApiDefinitionServiceMock.FetchDefinition.willReturnApiDefinition(helloApiDefinition)
      ApplicationIdsForCollaboratorFetcherMock

      val Some(result) = await(underTest(helloApiDefinition.serviceName, None))

      result.versions.head.productionAvailability mustBe None
      result.versions.head.sandboxAvailability mustBe Some(publicApiAvailability)
    }

  }
}
