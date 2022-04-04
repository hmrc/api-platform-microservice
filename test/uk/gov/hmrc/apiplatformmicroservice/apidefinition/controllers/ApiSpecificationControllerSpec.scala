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

package uk.gov.hmrc.apiplatformmicroservice.apidefinition.controllers

import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.mocks._
import uk.gov.hmrc.http.HeaderCarrier
import akka.stream.testkit.NoMaterializer
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.test.Helpers._
import play.api.libs.json.Json
import play.api.libs.json.JsValue
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiVersion
import play.api.test.FakeRequest

class ApiSpecificationControllerSpec extends AsyncHmrcSpec {
  
  trait Setup
      extends ApiSpecificationFetcherModule {
  
    implicit val headerCarrier = HeaderCarrier()
    implicit val mat = NoMaterializer

    val controller = new ApiSpecificationController(
      stubControllerComponents(),
      ApiSpecificationFetcherMock.aMock
    )

    val request = FakeRequest("GET", "/")
    val serviceName = "hello"
    val version = ApiVersion("1.0")
    val fakeResponse: JsValue = Json.parse("""{ "x" :1 }""")
  }

  "api specification fetcher" should {
    "find the relevant response" in new Setup {
      ApiSpecificationFetcherMock.Fetch.willReturn(fakeResponse)

      val result = controller.fetchApiSpecification(serviceName, version)(request)

      status(result) shouldBe OK
      contentAsString(result) shouldBe """{"x":1}"""
    }

    "returns not found when no such api version" in new Setup {
      ApiSpecificationFetcherMock.Fetch.willReturnNotFound


      val result = controller.fetchApiSpecification(serviceName, version)(request)

      status(result) shouldBe NOT_FOUND
    }
  }
}
