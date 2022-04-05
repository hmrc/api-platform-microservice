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

package uk.gov.hmrc.apiplatformmicroservice.apidefinition.services

import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models._
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiStatus.{RETIRED, STABLE, ALPHA}
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.mocks._
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.http.HeaderCarrier

class ApiIdentifiersForUpliftFetcherSpec extends AsyncHmrcSpec with ApiDefinitionTestDataHelper {

  private val versionOne = ApiVersion("1.0")
  private val versionTwo = ApiVersion("2.0")
  private val versionThree = ApiVersion("3.0")

  trait Setup extends ApiDefinitionServiceModule {
    val upliftableApiDefinition = apiDefinition("uplift", apiVersion(versionOne), apiVersion(versionTwo))
    val exampleApiDefinition = apiDefinition("hello-api").withCategories(List(ApiCategory("EXAMPLE")))
    val apiWithARetiredVersion = apiDefinition("api-with-retired-version", apiVersion(versionOne, RETIRED), apiVersion(versionTwo, STABLE), apiVersion(versionThree, ALPHA))
    val customsDeclarationsApiDefinition = apiDefinition("customs/declarations", apiVersion(versionOne))
    val testSupportApiDefinition = upliftableApiDefinition.isTestSupport()

    implicit val hc: HeaderCarrier = HeaderCarrier()

    val service = new EnvironmentAwareApiDefinitionService(SubordinateApiDefinitionServiceMock.aMock, PrincipalApiDefinitionServiceMock.aMock)

    val underTest = new ApiIdentifiersForUpliftFetcher(service)
    SubordinateApiDefinitionServiceMock.FetchAllApiDefinitions.willReturnNones()
  }

  "ApiIdentifiersForUpliftFetcher" should {
    "fetch nothing when no apis are suitable" in new Setup() {
      PrincipalApiDefinitionServiceMock.FetchAllApiDefinitions.willReturnNones()
      await(underTest.fetch) shouldBe Set.empty
    }

    "fetch nothing when only example apis are present" in new Setup() {
      PrincipalApiDefinitionServiceMock.FetchAllApiDefinitions.willReturn(exampleApiDefinition)
      await(underTest.fetch) shouldBe Set.empty
    }
    
    "fetch nothing when only test support apis are present" in new Setup() {
      PrincipalApiDefinitionServiceMock.FetchAllApiDefinitions.willReturn(testSupportApiDefinition)
      await(underTest.fetch) shouldBe Set.empty
    }
    
    "fetch an active version when api with retired version is present" in new Setup() {
      PrincipalApiDefinitionServiceMock.FetchAllApiDefinitions.willReturn(apiWithARetiredVersion)
      await(underTest.fetch) shouldBe Set("api-with-retired-version".asIdentifier(versionTwo))
    }

    "fetch all upliftable APIs when mix of all types are present" in new Setup() {
      PrincipalApiDefinitionServiceMock.FetchAllApiDefinitions.willReturn(upliftableApiDefinition, testSupportApiDefinition, apiWithARetiredVersion, exampleApiDefinition)
      await(underTest.fetch) shouldBe Set("uplift".asIdentifier, "uplift".asIdentifier(versionTwo), "api-with-retired-version".asIdentifier(versionTwo))
    }
  
    "fetch all upliftable APIs including additional special cases for CDS" in new Setup() {
      PrincipalApiDefinitionServiceMock.FetchAllApiDefinitions.willReturn(customsDeclarationsApiDefinition)

      await(underTest.fetch) shouldBe Set("customs/declarations".asIdentifier(), "customs/declarations".asIdentifier(versionTwo))
    }
  }
}