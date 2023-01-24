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

import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import play.api.libs.json.Json
import play.api.test.Helpers.{status, _}
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiDefinitionTestDataHelper
import uk.gov.hmrc.apiplatformmicroservice.common.builder.{ApplicationBuilder, CollaboratorsBuilder}
import uk.gov.hmrc.apiplatformmicroservice.common.connectors.AuthConnector
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.ApplicationId
import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.Role
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.services.ApplicationJsonFormatters
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks._

class ApplicationUpdateControllerSpec extends AsyncHmrcSpec with GuiceOneAppPerSuite with ApiDefinitionTestDataHelper {

  trait Setup extends ApplicationByIdFetcherModule with ApplicationUpdateServiceModule with ApplicationBuilder with CollaboratorsBuilder with ApplicationJsonFormatters {
    implicit val headerCarrier = HeaderCarrier()
    implicit val mat           = app.materializer

    val applicationId = ApplicationId.random

    val mockAuthConfig    = mock[AuthConnector.Config]
    val mockAuthConnector = mock[AuthConnector]

    val controller = new ApplicationUpdateController(
      mockAuthConfig,
      mockAuthConnector,
      ApplicationByIdFetcherMock.aMock,
      ApplicationUpdateServiceMock.aMock,
      Helpers.stubControllerComponents()
    )
  }

  "update" should {
    val payload =
      s"""{
         |        "actor": {
         |          "email": "someone@digital.hmrc.gov.uk",
         |          "actorType": "COLLABORATOR"
         |        }
         |        ,
         |        "collaboratorEmail": "thecollaborator@digital.hmrc.gov.uk"
         |        ,
         |        "collaboratorRole": "ADMINISTRATOR"
         |        ,
         |        "timestamp": "2022-10-12T08:06:46.706"
         |        ,
         |        "updateType": "addCollaboratorRequest"
         |      }""".stripMargin

    "return Ok when update service is successful" in new Setup {
      val application  = buildApplication(appId = applicationId)
      val collaborator = buildCollaborator("bob@example.com", Role.DEVELOPER)
      ApplicationByIdFetcherMock.FetchApplication.willReturnApplication(Option(application))
      val request      = FakeRequest("PATCH", s"/applications/${applicationId.value}")

      ApplicationUpdateServiceMock.UpdateApplication.willReturnApplication(application)

      val result = controller.update(applicationId)(request.withBody(Json.parse(payload)))

      status(result) shouldBe OK
    }

    "return Not Found when no application is found" in new Setup {
      val application  = buildApplication(appId = applicationId)
      val collaborator = buildCollaborator("bob@example.com", Role.DEVELOPER)
      ApplicationByIdFetcherMock.FetchApplication.willReturnApplication(None)
      val request      = FakeRequest("PATCH", s"/applications/${applicationId.value}")

      ApplicationUpdateServiceMock.UpdateApplication.willReturnApplication(application)

      val result = controller.update(applicationId)(request.withBody(Json.parse(payload)))

      status(result) shouldBe NOT_FOUND
    }

  }

}
