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

import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiDefinitionTestDataHelper
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks.ApplicationByIdFetcherModule
import play.api.libs.json.Json
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.DispatchRequest
import play.api.test.FakeRequest
import uk.gov.hmrc.apiplatformmicroservice.common.connectors._
import uk.gov.hmrc.apiplatformmicroservice.common.builder._
import uk.gov.hmrc.apiplatformmicroservice.common.utils._
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.services.ApplicationJsonFormatters
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.ApplicationId
import play.api.test.Helpers._
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models.Actors
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.Environment
import play.api.test.Helpers
import cats.data.NonEmptyChain
import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks.ApplicationCommandDispatcherMockModule

class ApplicationCommandControllerSpec extends AsyncHmrcSpec with ApiDefinitionTestDataHelper with FixedClock {
  
  trait Setup 
      extends ApplicationByIdFetcherModule
      with ApplicationBuilder
      with ApplicationCommandDispatcherMockModule
      with CollaboratorTracker
      with UpliftRequestSamples
      with ApplicationJsonFormatters {

    implicit val headerCarrier = HeaderCarrier()
    
    val applicationId = ApplicationId.random
    val application = buildApplication(appId = applicationId).copy(deployedTo = Environment.PRODUCTION)

    val adminEmail = "admin@example.com".toLaxEmail
    val developerAsCollaborator = "dev@example.com".toLaxEmail.asDeveloperCollaborator
    val verifiedEmails = Set.empty[LaxEmailAddress]

    val mockAuthConfig    = mock[AuthConnector.Config]
    val mockAuthConnector = mock[AuthConnector]
    
    val controller: ApplicationCommandController = new ApplicationCommandController(ApplicationByIdFetcherMock.aMock, mockAuthConfig, mockAuthConnector, ApplicationCommandDispatcherMock.aMock, Helpers.stubControllerComponents())
  }

  "ApplicationCommandController" should {
    import cats.syntax.option._

    "dispatch a command when the app exists" in new Setup{

      ApplicationByIdFetcherMock.FetchApplication.willReturnApplication(application.some)
      ApplicationCommandDispatcherMock.IssueCommand.Dispatch.succeedsWith(application)

      val cmd = ApplicationCommands.AddCollaborator(Actors.AppCollaborator(adminEmail), developerAsCollaborator, now)
      val request = FakeRequest("PATCH", s"/applications/${applicationId.value}/dispatch").withBody(Json.toJson(DispatchRequest(cmd,verifiedEmails)))
      
      status(controller.dispatch(applicationId)(request)) shouldBe OK

      ApplicationCommandDispatcherMock.IssueCommand.verifyCalledWith(cmd, verifiedEmails)
    }
    
    "dispatch a command and handle command failure" in new Setup{

      ApplicationByIdFetcherMock.FetchApplication.willReturnApplication(application.some)
      ApplicationCommandDispatcherMock.IssueCommand.Dispatch.failsWith(CommandFailures.ActorIsNotACollaboratorOnApp)

      val cmd = ApplicationCommands.AddCollaborator(Actors.AppCollaborator(adminEmail), developerAsCollaborator, now)
      val request = FakeRequest("PATCH", s"/applications/${applicationId.value}/dispatch").withBody(Json.toJson(DispatchRequest(cmd,verifiedEmails)))
      
      val result = controller.dispatch(applicationId)(request)
      status(result) shouldBe BAD_REQUEST

      import uk.gov.hmrc.apiplatform.modules.common.domain.services.NonEmptyChainFormatters._
      Json.fromJson[NonEmptyChain[CommandFailure]](contentAsJson(result)).get shouldBe NonEmptyChain.one(CommandFailures.ActorIsNotACollaboratorOnApp)

      ApplicationCommandDispatcherMock.IssueCommand.verifyCalledWith(cmd, verifiedEmails)
    }

    "dont dispatch command when the app does not exist" in new Setup{

      ApplicationByIdFetcherMock.FetchApplication.willReturnApplication(None)

      val cmd = ApplicationCommands.AddCollaborator(Actors.AppCollaborator(adminEmail), developerAsCollaborator, now)
      val request = FakeRequest("PATCH", s"/applications/${applicationId.value}/dispatch").withBody(Json.toJson(DispatchRequest(cmd,verifiedEmails)))
      
      status(controller.dispatch(applicationId)(request)) shouldBe BAD_REQUEST

      ApplicationCommandDispatcherMock.IssueCommand.verifyNoCommandsIssued()
    }    
  }
}
