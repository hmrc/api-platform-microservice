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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.controllers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
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

import scala.concurrent.ExecutionContext.Implicits.global

class ApplicationControllerSpec extends AsyncHmrcSpec with GuiceOneAppPerSuite {

  trait Setup extends ApplicationByIdFetcherModule with ApplicationCollaboratorServiceModule with ApplicationBuilder with CollaboratorsBuilder {
    implicit val headerCarrier = HeaderCarrier()
    implicit val system = ActorSystem("test")
    implicit val mat = ActorMaterializer()

    val applicationId = ApplicationId.random

    val mockAuthConfig = mock[AuthConnector.Config]
    val mockAuthConnector = mock[AuthConnector]

    val controller = new ApplicationController(
      ApplicationByIdFetcherMock.aMock,
      mockAuthConfig,
      mockAuthConnector,
      ApplicationCollaboratorServiceMock.aMock,
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
}