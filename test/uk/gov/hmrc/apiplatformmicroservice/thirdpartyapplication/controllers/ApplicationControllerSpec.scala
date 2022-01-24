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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.controllers

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Json
import play.api.test.Helpers.{status, _}
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.apiplatformmicroservice.common.builder.{ApplicationBuilder, CollaboratorsBuilder}
import uk.gov.hmrc.apiplatformmicroservice.common.connectors.AuthConnector
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.{AddCollaboratorSuccessResult, CollaboratorAlreadyExistsFailureResult}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.Role
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks._
import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.http.HeaderCarrier
import scala.concurrent.Future.successful
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.UpliftApplicationService
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiDefinitionTestDataHelper
import uk.gov.hmrc.apiplatformmicroservice.common.utils.UpliftRequestSamples

class ApplicationControllerSpec extends AsyncHmrcSpec with GuiceOneAppPerSuite with ApiDefinitionTestDataHelper {

  trait Setup extends ApplicationByIdFetcherModule with ApplicationCollaboratorServiceModule with ApplicationBuilder with CollaboratorsBuilder with UpliftRequestSamples {
    implicit val headerCarrier = HeaderCarrier()
    implicit val mat = app.materializer

    val applicationId = ApplicationId.random

    val mockAuthConfig = mock[AuthConnector.Config]
    val mockAuthConnector = mock[AuthConnector]
    val mockUpliftService = mock[UpliftApplicationService]

    val controller = new ApplicationController(
      ApplicationByIdFetcherMock.aMock,
      mockAuthConfig,
      mockAuthConnector,
      ApplicationCollaboratorServiceMock.aMock,
      mockUpliftService,
      Helpers.stubControllerComponents()
    )
  }

  "addCollaborator" should {
    "return Created when successfully adding a Collaborator" in new Setup {
      val application = buildApplication(appId = applicationId)
      val collaborator = buildCollaborator("bob@example.com", Role.DEVELOPER)
      ApplicationByIdFetcherMock.FetchApplication.willReturnApplication(application)
      val request = FakeRequest("POST", s"/applications/${applicationId.value}/collaborators")
      val payload = s"""{"email":"${collaborator.emailAddress}", "role":"${collaborator.role.toString}"}"""
      val response = AddCollaboratorSuccessResult(true)

      ApplicationCollaboratorServiceMock.AddCollaborator.willReturnAddCollaboratorResponse(response)

      val result = controller.addCollaborator(applicationId)(request.withBody(Json.parse(payload)))

      status(result) shouldBe CREATED
    }

    "return Conflict when Collaborator already exists" in new Setup {
      val application = buildApplication(appId = applicationId)
      val collaborator = buildCollaborator("bob@example.com", Role.DEVELOPER)
      ApplicationByIdFetcherMock.FetchApplication.willReturnApplication(application)
      val request = FakeRequest("POST", s"/applications/${applicationId.value}/collaborators")
      val payload = s"""{"email":"${collaborator.emailAddress}", "role":"${collaborator.role.toString}"}"""
      val response = CollaboratorAlreadyExistsFailureResult

      ApplicationCollaboratorServiceMock.AddCollaborator.willReturnAddCollaboratorResponse(response)

      val result = controller.addCollaborator(applicationId)(request.withBody(Json.parse(payload)))

      status(result) shouldBe CONFLICT
    }
  }

  "upliftApplicationV2" should {
    implicit val writes = Json.writes[ApplicationController.RequestUpliftV2]
    val newAppId = ApplicationId.random
    val apiId1 = "context1".asIdentifier

    "return Created when successfully uplifting an Application" in new Setup {
      val application = buildApplication(appId = applicationId)
      
      ApplicationByIdFetcherMock.FetchApplicationWithSubscriptionData.willReturnApplicationWithSubscriptionData(application, Set(apiId1))
      when(mockUpliftService.upliftApplicationV2(*, *, *)(*)).thenReturn(successful(Right(newAppId)))

      val request = FakeRequest("POST", s"/applications/${applicationId.value}/uplift").withBody(Json.toJson(ApplicationController.RequestUpliftV2(makeUpliftRequest(apiId1))))

      val result = controller.upliftApplication(applicationId)(request)

      status(result) shouldBe CREATED
      contentAsJson(result) shouldBe(Json.toJson(newAppId))
    }
  }
  
  "upliftApplicationV1" should {
    implicit val writes = Json.writes[ApplicationController.RequestUpliftV1]
    val newAppId = ApplicationId.random
    val apiId1 = "context1".asIdentifier

    "return Created when successfully uplifting an Application" in new Setup {
      val application = buildApplication(appId = applicationId)
      
      ApplicationByIdFetcherMock.FetchApplicationWithSubscriptionData.willReturnApplicationWithSubscriptionData(application, Set(apiId1))
      when(mockUpliftService.upliftApplicationV1(*, *, *)(*)).thenReturn(successful(Right(newAppId)))

      val request = FakeRequest("POST", s"/applications/${applicationId.value}/uplift").withBody(Json.toJson(ApplicationController.RequestUpliftV1(Set(apiId1))))

      val result = controller.upliftApplication(applicationId)(request)

      status(result) shouldBe CREATED
      contentAsJson(result) shouldBe(Json.toJson(newAppId))
    }
  }
}