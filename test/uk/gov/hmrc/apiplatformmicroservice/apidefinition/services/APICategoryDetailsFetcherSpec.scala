/*
 * Copyright 2021 HM Revenue & Customs
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
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.{APICategoryDetails, ApiDefinitionTestDataHelper}
import uk.gov.hmrc.apiplatformmicroservice.util.AsyncHmrcSpec
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class APICategoryDetailsFetcherSpec extends AsyncHmrcSpec with ApiDefinitionTestDataHelper {

  trait Setup extends ApiDefinitionServiceModule {
    implicit val headerCarrier = HeaderCarrier()

    val fetcherUnderTest =
      new APICategoryDetailsFetcher(
        new EnvironmentAwareApiDefinitionService(
          SubordinateApiDefinitionServiceMock.aMock,
          PrincipalApiDefinitionServiceMock.aMock))
  }

  "APICategoryDetailsFetcher" should {
    val subordinateEnvironmentOnlyCategory = APICategoryDetails("SUBORDINATE_CATEGORY", "API Category in Subordinate Environment")
    val principalEnvironmentOnlyCategory = APICategoryDetails("PRINCIPAL_CATEGORY", "API Category in Principal Environment")
    val category1 = APICategoryDetails("API_CATEGORY_1", "API Category 1")
    val category2 = APICategoryDetails("API_CATEGORY_2", "API Category 2")

    "retrieve all API Categories from PRODUCTION and SANDBOX environments and combine them" in new Setup {
      SubordinateApiDefinitionServiceMock.FetchApiCategoryDetails.willReturnAPICategoryDetails(subordinateEnvironmentOnlyCategory, category1, category2)
      PrincipalApiDefinitionServiceMock.FetchApiCategoryDetails.willReturnAPICategoryDetails(principalEnvironmentOnlyCategory, category1, category2)

      val result = await(fetcherUnderTest.fetch())

      result.size must be (4)
      result must contain only (subordinateEnvironmentOnlyCategory, principalEnvironmentOnlyCategory, category1, category2)
    }
  }
}
