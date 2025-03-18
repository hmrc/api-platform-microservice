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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.controllers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future.successful

import play.api.libs.json.Json
import play.api.test.Helpers.{status, _}
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationWithCollaboratorsFixtures
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiDefinitionTestDataHelper
import uk.gov.hmrc.apiplatformmicroservice.common.connectors.AuthConnector
import uk.gov.hmrc.apiplatformmicroservice.common.utils.{AsyncHmrcSpec, UpliftRequestSamples}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.services.ApplicationJsonFormatters
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks._
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.UpliftApplicationService

class ApplicationControllerSpec extends AsyncHmrcSpec with ApiDefinitionTestDataHelper with ApplicationWithCollaboratorsFixtures {

  trait Setup extends ApplicationByIdFetcherModule with ApplicationWithCollaboratorsFixtures with UpliftRequestSamples
      with SubordinateApplicationFetcherModule with ApplicationJsonFormatters {
    implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

    val mockAuthConfig    = mock[AuthConnector.Config]
    val mockAuthConnector = mock[AuthConnector]
    val mockUpliftService = mock[UpliftApplicationService]

    val controller = new ApplicationController(
      ApplicationByIdFetcherMock.aMock,
      mockAuthConfig,
      mockAuthConnector,
      mockUpliftService,
      SubordinateApplicationFetcherMock.aMock,
      Helpers.stubControllerComponents()
    )
  }

  "upliftApplicationV2" should {
    implicit val writes = Json.writes[ApplicationController.RequestUpliftV2]
    val newAppId        = applicationIdTwo
    val apiId1          = "context1".asIdentifier()

    "return Created when successfully uplifting an Application" in new Setup {
      val application = standardApp.inSandbox().withId(applicationIdOne)

      ApplicationByIdFetcherMock.FetchApplicationWithSubscriptionData.willReturnApplicationWithSubscriptionData(application, Set(apiId1))
      when(mockUpliftService.upliftApplicationV2(*, *, *)(*)).thenReturn(successful(Right(newAppId)))

      val request = FakeRequest("POST", s"/applications/${applicationIdOne}/uplift").withBody(Json.toJson(ApplicationController.RequestUpliftV2(makeUpliftRequest(apiId1))))

      val result = controller.upliftApplication(applicationIdOne)(request)

      status(result) shouldBe CREATED
      contentAsJson(result) shouldBe (Json.toJson(newAppId))
    }
  }

  "fetchLinkedSubordinateApplication" should {
    val principalAppId   = ApplicationId.random
    val subordinateAppId = ApplicationId.random

    "return 200 if subordinate application is found" in new Setup {
      val subordinateApplication = standardApp.inSandbox().withId(subordinateAppId)
      SubordinateApplicationFetcherMock.FetchSubordinateApplication.willReturnApplication(subordinateApplication)

      val result = controller.fetchLinkedSubordinateApplication(principalAppId)(FakeRequest("GET", "/"))

      status(result) shouldBe OK
      contentAsJson(result) shouldBe (Json.toJson(subordinateApplication))
    }
    "return 404 if subordinate application is not found" in new Setup {
      SubordinateApplicationFetcherMock.FetchSubordinateApplication.willReturnNothing

      val result = controller.fetchLinkedSubordinateApplication(principalAppId)(FakeRequest("GET", "/"))

      status(result) shouldBe NOT_FOUND
    }
  }
}
