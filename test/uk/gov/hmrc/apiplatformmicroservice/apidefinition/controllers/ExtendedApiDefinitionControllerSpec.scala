/*
 * Copyright 2023 HM Revenue & Customs
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

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.testkit.NoMaterializer

import play.api.http.HeaderNames
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, InternalServerException}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApiVersionNbr, UserId}
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ServiceName
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.mocks._
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiDefinitionTestDataHelper
import uk.gov.hmrc.apiplatformmicroservice.common.StreamedResponseHelper.PROXY_SAFE_CONTENT_TYPE
import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec

class ExtendedApiDefinitionControllerSpec extends AsyncHmrcSpec with ApiDefinitionTestDataHelper {

  trait Setup
      extends ApiDefinitionsForCollaboratorFetcherModule
      with ExtendedApiDefinitionForCollaboratorFetcherModule
      with ApiDocumentationResourceFetcherModule
      with SubscribedApiDefinitionsForCollaboratorFetcherModule {
    implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
    implicit val mat: Materializer            = NoMaterializer

    val request                 = FakeRequest("GET", "/")
    val apiName                 = "hello-api"
    val serviceName             = ServiceName("hello-api")
    val version                 = ApiVersionNbr("1.0")
    val anApiDefinition         = apiDefinition(apiName)
    val anExtendedApiDefinition = extendedApiDefinition(apiName)

    val controller       = new ExtendedApiDefinitionController(
      Helpers.stubControllerComponents(),
      ApiDefinitionsForCollaboratorFetcherMock.aMock,
      ExtendedApiDefinitionForCollaboratorFetcherMock.aMock,
      ApiDocumentationResourceFetcherMock.aMock,
      SubscribedApiDefinitionsForCollaboratorFetcherMock.aMock
    )
    val mockHttpResponse = mock[HttpResponse]
    when(mockHttpResponse.status).thenReturn(OK)
    when(mockHttpResponse.headers).thenReturn(
      Map(
        "Content-Length" -> Seq("500")
      )
    )
    when(mockHttpResponse.header(eqTo(PROXY_SAFE_CONTENT_TYPE))).thenReturn(None)
    when(mockHttpResponse.header(eqTo(HeaderNames.CONTENT_TYPE))).thenReturn(Some("application/json"))
  }
  "fetchApiDefinitionsForCollaborator" should {
    val userId = Some(UserId.random)

    "return the API definitions when email provided" in new Setup {
      ApiDefinitionsForCollaboratorFetcherMock.willReturnApiDefinitions(anApiDefinition)

      val result = controller.fetchApiDefinitionsForCollaborator(userId)(request)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(List(anApiDefinition))
    }

    "return the API definitions when no email provided" in new Setup {
      ApiDefinitionsForCollaboratorFetcherMock.willReturnApiDefinitions(anApiDefinition)

      val result = controller.fetchApiDefinitionsForCollaborator(None)(request)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(List(anApiDefinition))
    }

    "return an empty when there are no api definitions available" in new Setup {
      ApiDefinitionsForCollaboratorFetcherMock.willReturnApiDefinitions(List.empty: _*)

      val result = controller.fetchApiDefinitionsForCollaborator(userId)(request)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.parse("[]")
    }

    "return error when the service throws and exception" in new Setup {
      ApiDefinitionsForCollaboratorFetcherMock.willThrowException(new RuntimeException("Something went wrong oops..."))

      val result = controller.fetchApiDefinitionsForCollaborator(userId)(request)

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsJson(result) mustBe Json.obj("code" -> "UNKNOWN_ERROR", "message" -> "An unexpected error occurred")
    }
  }

  "fetchSubscribedApiDefinitionsForCollaborator" should {
    val userId = UserId.random

    "return the API definitions the user is subscribed to" in new Setup {
      SubscribedApiDefinitionsForCollaboratorFetcherMock.willReturnApiDefinitions(anApiDefinition)

      val result = controller.fetchSubscribedApiDefinitionsForCollaborator(userId)(request)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(List(anApiDefinition))
    }

    "return an empty List when there are no api definitions available" in new Setup {
      SubscribedApiDefinitionsForCollaboratorFetcherMock.willReturnApiDefinitions(List.empty: _*)

      val result = controller.fetchSubscribedApiDefinitionsForCollaborator(userId)(request)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.parse("[]")
    }

    "return error when the service throws and exception" in new Setup {
      SubscribedApiDefinitionsForCollaboratorFetcherMock.willThrowException(new RuntimeException("Something went wrong oops..."))

      val result = controller.fetchSubscribedApiDefinitionsForCollaborator(userId)(request)

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsJson(result) mustBe Json.obj("code" -> "UNKNOWN_ERROR", "message" -> "An unexpected error occurred")
    }
  }

  "fetchExtendedApiDefinitionForCollaborator" should {
    val userId = Some(UserId.random)

    "return the extended API definition when email provided" in new Setup {
      ExtendedApiDefinitionForCollaboratorFetcherMock.Fetch.willReturnExtendedApiDefinition(anExtendedApiDefinition)

      val result = controller.fetchExtendedApiDefinitionForCollaborator(serviceName, userId)(request)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(anExtendedApiDefinition)
    }

    "return the extended API definition when no email provided" in new Setup {
      ExtendedApiDefinitionForCollaboratorFetcherMock.Fetch.willReturnExtendedApiDefinition(anExtendedApiDefinition)

      val result = controller.fetchExtendedApiDefinitionForCollaborator(serviceName, None)(request)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(anExtendedApiDefinition)
    }

    "return 404 when there is no matching API definition" in new Setup {
      ExtendedApiDefinitionForCollaboratorFetcherMock.Fetch.willReturnNoExtendedApiDefinition()

      val result = controller.fetchExtendedApiDefinitionForCollaborator(serviceName, userId)(request)

      status(result) mustBe NOT_FOUND
    }

    "return error when the service throws and exception" in new Setup {
      ExtendedApiDefinitionForCollaboratorFetcherMock.Fetch.willThrowException(new RuntimeException("Something went wrong oops..."))

      val result = controller.fetchExtendedApiDefinitionForCollaborator(serviceName, userId)(request)

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsJson(result) mustBe Json.obj("code" -> "UNKNOWN_ERROR", "message" -> "An unexpected error occurred")
    }
  }

  "fetchApiDocumentationResource" should {
    "return resource when found" in new Setup {
      ApiDocumentationResourceFetcherMock.willReturnWsResponse(mockHttpResponse)

      val result: Future[Result] =
        controller.fetchApiDocumentationResource(serviceName, version, "some/resource")(request)

      status(result) shouldEqual OK
    }

    "return resource using content type when no proxy safe content type is present" in new Setup {
      ApiDocumentationResourceFetcherMock.willReturnWsResponse(mockHttpResponse)

      when(mockHttpResponse.header(eqTo(PROXY_SAFE_CONTENT_TYPE))).thenReturn(None)
      when(mockHttpResponse.header(eqTo(HeaderNames.CONTENT_TYPE))).thenReturn(Some("application/magic"))

      val result: Future[Result] =
        controller.fetchApiDocumentationResource(serviceName, version, "some/resource")(request)

      contentType(result) shouldEqual Some("application/magic")
    }

    "return resource using proxy safe content type when present" in new Setup {
      ApiDocumentationResourceFetcherMock.willReturnWsResponse(mockHttpResponse)

      when(mockHttpResponse.header(eqTo(HeaderNames.CONTENT_TYPE))).thenReturn(Some("application/zip"))

      val result: Future[Result] =
        controller.fetchApiDocumentationResource(serviceName, version, "some/resource")(request)

      contentType(result) shouldEqual Some("application/zip")
    }

    "return not found when not found" in new Setup {
      ApiDocumentationResourceFetcherMock.willReturnNone()

      val result = controller.fetchApiDocumentationResource(
        serviceName,
        version,
        "some/resourceNotThere"
      )(request)

      status(result) shouldBe NOT_FOUND
    }

    "throw InternalServerException for any other response" in new Setup {
      ApiDocumentationResourceFetcherMock.willThrowException(new InternalServerException("Unexpected Error"))

      intercept[InternalServerException] {
        await(
          controller.fetchApiDocumentationResource(
            serviceName,
            version,
            "some/resourceInvalid"
          )(request)
        )
      }
    }
  }
}
