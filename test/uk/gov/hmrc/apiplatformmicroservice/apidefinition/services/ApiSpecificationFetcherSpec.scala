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
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiDefinitionTestDataHelper
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.mocks.ExtendedApiDefinitionForCollaboratorFetcherModule
import scala.concurrent.ExecutionContext.Implicits.global
import akka.stream.testkit.NoMaterializer
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.mocks.ApiDefinitionServiceModule
import uk.gov.hmrc.http.HeaderCarrier
import play.api.libs.json.Json
import play.api.libs.json.JsValue
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ExtendedApiDefinitionExampleData

class ApiSpecificationFetcherSpec extends AsyncHmrcSpec with ApiDefinitionTestDataHelper with ExtendedApiDefinitionExampleData {

  trait Setup extends ExtendedApiDefinitionForCollaboratorFetcherModule with ApiDefinitionServiceModule {
    val environmentAwareApiDefinitionService = new EnvironmentAwareApiDefinitionService(SubordinateApiDefinitionServiceMock.aMock, PrincipalApiDefinitionServiceMock.aMock)

    implicit val headerCarrier = HeaderCarrier()
    implicit val mat = NoMaterializer
    val serviceName = apiName

    val fetcher = new ApiSpecificationFetcher(environmentAwareApiDefinitionService, ExtendedApiDefinitionForCollaboratorFetcherMock.aMock)
  }

 "api specification fetcher" should {
   val someJsValue: JsValue = Json.parse("""{ "x": 1 }""")
   val expectedJsValue: JsValue = Json.parse(Json.stringify(someJsValue))
    
  "fetch data when in subordinate" in new Setup {
    ExtendedApiDefinitionForCollaboratorFetcherMock.willReturnExtendedApiDefinition(anExtendedApiDefinitionWithOnlySubordinate)
    SubordinateApiDefinitionServiceMock.FetchApiSpecification.willReturn(someJsValue)
    PrincipalApiDefinitionServiceMock.FetchApiSpecification.willReturnNone

    val result = await(fetcher.fetch(serviceName, versionOne))

    println(expectedJsValue)
    result shouldBe Some(expectedJsValue)
  }
    
  "fetch data when in both" in new Setup {
    ExtendedApiDefinitionForCollaboratorFetcherMock.willReturnExtendedApiDefinition(anExtendedApiDefinitionWithPrincipalAndSubordinate)
    SubordinateApiDefinitionServiceMock.FetchApiSpecification.willReturn(someJsValue)
    PrincipalApiDefinitionServiceMock.FetchApiSpecification.willReturn(someJsValue)

    val result = await(fetcher.fetch(serviceName, versionOne))

    println(expectedJsValue)
    result shouldBe Some(expectedJsValue)
  }

  "fetch data when in principal" in new Setup {
    ExtendedApiDefinitionForCollaboratorFetcherMock.willReturnExtendedApiDefinition(anExtendedApiDefinitionWithOnlyPrincipal)
    SubordinateApiDefinitionServiceMock.FetchApiSpecification.willReturnNone
    PrincipalApiDefinitionServiceMock.FetchApiSpecification.willReturn(someJsValue)

    val result = await(fetcher.fetch(serviceName, versionOne))

    result shouldBe Some(expectedJsValue)
  }
 } 
}
