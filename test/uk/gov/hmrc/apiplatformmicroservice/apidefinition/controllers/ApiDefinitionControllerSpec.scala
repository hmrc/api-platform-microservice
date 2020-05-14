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

package uk.gov.hmrc.apiplatformmicroservice.apidefinition.controllers

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.JsonFormatters._
import uk.gov.hmrc.apiplatformmicroservice.util.{ApiDefinitionTestDataHelper, AsyncHmrcSpec, ApiDefinitionsForCollaboratorFetcherModule}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class ApiDefinitionControllerSpec extends AsyncHmrcSpec with GuiceOneAppPerSuite with ApiDefinitionTestDataHelper {

  trait Setup extends ApiDefinitionsForCollaboratorFetcherModule {
    implicit val headerCarrier = HeaderCarrier()
    val fakeRequest = FakeRequest("GET", "/")
    val fakeEmail = "joebloggs@example.com"
    val fakeApiName = "hello-api"
    val fakeApiDefinition = apiDefinition(fakeApiName)
    val controller = new ApiDefinitionController(Helpers.stubControllerComponents(), ApiDefinitionsForCollaboratorFetcherMock.aMock)
  }

  "ApiDefinitionController" should {
    "return the API definitions" in new Setup {
      ApiDefinitionsForCollaboratorFetcherMock.returnApiDefinitions(fakeApiDefinition)

      val result = controller.fetchApiDefinitionsForCollaborator(fakeEmail)(fakeRequest)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(Seq(fakeApiDefinition))
    }

    "return an empty when there are no api definitions available" in new Setup {
      ApiDefinitionsForCollaboratorFetcherMock.returnApiDefinitions(Seq.empty: _*)

      val result = controller.fetchApiDefinitionsForCollaborator(fakeEmail)(fakeRequest)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.parse("[]")
    }

    "return error when the service throws and exception" in new Setup {
      ApiDefinitionsForCollaboratorFetcherMock.throwException(new RuntimeException("Something went wrong oops..."))

      val result = controller.fetchApiDefinitionsForCollaborator(fakeEmail)(fakeRequest)

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsJson(result) mustBe Json.obj("code" -> "UNKNOWN_ERROR",
                                                   "message" -> "An unexpected error occurred")
    }

  }
}
