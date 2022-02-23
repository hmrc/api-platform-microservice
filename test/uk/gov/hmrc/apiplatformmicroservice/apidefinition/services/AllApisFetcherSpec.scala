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

import uk.gov.hmrc.apiplatformmicroservice.apidefinition.mocks.ApiDefinitionServiceModule
import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiDefinitionTestDataHelper

import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiCategory

class AllApisFetcherSpec extends AsyncHmrcSpec with ApiDefinitionTestDataHelper  {
    
trait Setup extends ApiDefinitionServiceModule {
    implicit val hc = HeaderCarrier()
    val inTest = new AllApisFetcher(PrincipalApiDefinitionServiceMock.aMock,
       SubordinateApiDefinitionServiceMock.aMock)

    val exampleApiDefinition1 = apiDefinition("hello-api").withCategories(List(ApiCategory("EXAMPLE")))
    val exampleApiDefinition2 = exampleApiDefinition1.copy(serviceName = "hello-api-2")
}

    "fetch" should {

        "fetch distinct combined apis when they're present in both environment" in new Setup() {
            PrincipalApiDefinitionServiceMock.FetchAllApiDefinitions.willReturn(exampleApiDefinition1, exampleApiDefinition2)
            SubordinateApiDefinitionServiceMock.FetchAllApiDefinitions.willReturn(exampleApiDefinition2)
            await(inTest.fetch) should contain only (exampleApiDefinition2, exampleApiDefinition1)
        }
        "fetch distinct combined apis when apis only present in Principal environment" in new Setup() {
            PrincipalApiDefinitionServiceMock.FetchAllApiDefinitions.willReturn(exampleApiDefinition1, exampleApiDefinition2)
            SubordinateApiDefinitionServiceMock.FetchAllApiDefinitions.willReturnNoApiDefinitions()
            await(inTest.fetch) should contain only (exampleApiDefinition2, exampleApiDefinition1)
        }
        "fetch distinct combined apis when apis only present in Subordinate environment" in new Setup() {
          PrincipalApiDefinitionServiceMock.FetchAllApiDefinitions.willReturnNoApiDefinitions()
          SubordinateApiDefinitionServiceMock.FetchAllApiDefinitions.willReturn(exampleApiDefinition1, exampleApiDefinition2)
          await(inTest.fetch) should contain only (exampleApiDefinition2, exampleApiDefinition1)
        }
        "return empty list when no apis only present in either environment" in new Setup() {
          PrincipalApiDefinitionServiceMock.FetchAllApiDefinitions.willReturnNoApiDefinitions()
          SubordinateApiDefinitionServiceMock.FetchAllApiDefinitions.willReturnNoApiDefinitions()
          await(inTest.fetch) shouldBe Nil
        }
    }
}
