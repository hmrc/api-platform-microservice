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
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.mocks.{ApiDefinitionsForCollaboratorFetcherModule, ExtendedApiDefinitionForCollaboratorFetcherModule}
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiDefinitionTestDataHelper
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.JsonFormatters._
import uk.gov.hmrc.apiplatformmicroservice.util.AsyncHmrcSpec
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class ApiDefinitionControllerSpec extends AsyncHmrcSpec with GuiceOneAppPerSuite with ApiDefinitionTestDataHelper {

  trait Setup extends ApiDefinitionsForCollaboratorFetcherModule with ExtendedApiDefinitionForCollaboratorFetcherModule {
    implicit val headerCarrier = HeaderCarrier()
    val request = FakeRequest("GET", "/")
    val email = Some("joebloggs@example.com")
    val apiName = "hello-api"
    val anApiDefinition = apiDefinition(apiName)
    val anExtendedApiDefinition = extendedApiDefinition(apiName)
    val controller = new ApiDefinitionController(Helpers.stubControllerComponents(),
      ApiDefinitionsForCollaboratorFetcherMock.aMock, ExtendedApiDefinitionForCollaboratorFetcherMock.aMock)
  }

  "fetchApiDefinitionsForCollaborator" should {
    "return the API definitions when email provided" in new Setup {
      ApiDefinitionsForCollaboratorFetcherMock.willReturnApiDefinitions(anApiDefinition)

      val result = controller.fetchApiDefinitionsForCollaborator(email)(request)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(Seq(anApiDefinition))
    }

    "return the API definitions when no email provided" in new Setup {
      ApiDefinitionsForCollaboratorFetcherMock.willReturnApiDefinitions(anApiDefinition)

      val result = controller.fetchApiDefinitionsForCollaborator(None)(request)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(Seq(anApiDefinition))
    }

    "return an empty when there are no api definitions available" in new Setup {
      ApiDefinitionsForCollaboratorFetcherMock.willReturnApiDefinitions(Seq.empty: _*)

      val result = controller.fetchApiDefinitionsForCollaborator(email)(request)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.parse("[]")
    }

    "return error when the service throws and exception" in new Setup {
      ApiDefinitionsForCollaboratorFetcherMock.willThrowException(new RuntimeException("Something went wrong oops..."))

      val result = controller.fetchApiDefinitionsForCollaborator(email)(request)

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsJson(result) mustBe Json.obj("code" -> "UNKNOWN_ERROR",
                                                   "message" -> "An unexpected error occurred")
    }
  }

  "fetchExtendedApiDefinitionForCollaborator" should {
    "return the extended API definition when email provided" in new Setup {
      ExtendedApiDefinitionForCollaboratorFetcherMock.willReturnExtendedApiDefinition(anExtendedApiDefinition)

      val result = controller.fetchExtendedApiDefinitionForCollaborator(apiName, email)(request)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(anExtendedApiDefinition)
    }

    "return the extended API definition when no email provided" in new Setup {
      ExtendedApiDefinitionForCollaboratorFetcherMock.willReturnExtendedApiDefinition(anExtendedApiDefinition)

      val result = controller.fetchExtendedApiDefinitionForCollaborator(apiName, None)(request)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(anExtendedApiDefinition)
    }

    "return 404 when there is no matching API definition" in new Setup {
      ExtendedApiDefinitionForCollaboratorFetcherMock.willReturnNoExtendedApiDefinition()

      val result = controller.fetchExtendedApiDefinitionForCollaborator(apiName, email)(request)

      status(result) mustBe NOT_FOUND
    }

    "return error when the service throws and exception" in new Setup {
      ExtendedApiDefinitionForCollaboratorFetcherMock.willThrowException(new RuntimeException("Something went wrong oops..."))

      val result = controller.fetchExtendedApiDefinitionForCollaborator(apiName, email)(request)

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsJson(result) mustBe Json.obj("code" -> "UNKNOWN_ERROR",
        "message" -> "An unexpected error occurred")
    }
  }
}
