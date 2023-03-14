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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global

import org.joda.time.DateTime
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import uk.gov.hmrc.apiplatformmicroservice.common.builder.{ApplicationBuilder, UserResponseBuilder}
import uk.gov.hmrc.apiplatform.modules.developers.domain.models.UserId
import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.domain.UnregisteredUserResponse
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.Role.DEVELOPER
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications._
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks._

class ApplicationUpdateServiceSpec extends AsyncHmrcSpec {

  implicit val hc = HeaderCarrier()

  trait Setup extends ThirdPartyApplicationConnectorModule with ApplicationCollaboratorServiceModule with MockitoSugar
      with ArgumentMatchersSugar with UserResponseBuilder with ApplicationBuilder {

    val service = new ApplicationUpdateService(ApplicationCollaboratorServiceMock.aMock, EnvironmentAwareThirdPartyApplicationConnectorMock.instance)

    val newCollaboratorEmail                    = "newCollaborator@testuser.com"
    val newCollaboratorUserId                   = UserId.random
    val newCollaborator                         = Collaborator(newCollaboratorEmail, Role.DEVELOPER, Some(newCollaboratorUserId))
    val newCollaboratorUserResponse             = buildUserResponse(email = newCollaboratorEmail, userId = newCollaboratorUserId)
    val newCollaboratorUnregisteredUserResponse = UnregisteredUserResponse(newCollaboratorEmail, DateTime.now, newCollaboratorUserId)

    val requesterEmail            = "adminRequester@example.com"
    val verifiedAdminEmail        = "verifiedAdmin@example.com"
    val unverifiedAdminEmail      = "unverifiedAdmin@example.com"
    val adminEmailsMinusRequester = Set(verifiedAdminEmail, unverifiedAdminEmail)
    val adminEmails               = Set(verifiedAdminEmail, unverifiedAdminEmail, requesterEmail)

    val adminMinusRequesterUserResponses =
      Seq(buildUserResponse(email = verifiedAdminEmail, userId = UserId.random), buildUserResponse(email = unverifiedAdminEmail, verified = false, userId = UserId.random))

    val adminUserResponses = Seq(
      buildUserResponse(email = verifiedAdminEmail, userId = UserId.random),
      buildUserResponse(email = unverifiedAdminEmail, verified = false, userId = UserId.random),
      buildUserResponse(email = requesterEmail, userId = UserId.random)
    )

    val existingAdminCollaborators = Set(
      Collaborator(verifiedAdminEmail, Role.ADMINISTRATOR, None),
      Collaborator(unverifiedAdminEmail, Role.ADMINISTRATOR, None),
      Collaborator(requesterEmail, Role.ADMINISTRATOR, None)
    )

    val existingCollaborators: Set[Collaborator] = existingAdminCollaborators ++ Set(Collaborator("collaborator1@example.com", Role.DEVELOPER, None))
    val productionApplication                    = buildApplication().deployedToProduction.withCollaborators(existingCollaborators)

  }

  "addCollaborator" should {
    val actor        = CollaboratorActor("someEMail")
    val collaborator = Collaborator("collaboratorEmail", DEVELOPER, Option(UserId.random))
    val request      = AddCollaboratorRequest(actor, collaborator.emailAddress, collaborator.role, LocalDateTime.now())
    "call third party application with decorated AddCollaborator when called" in new Setup {

      ApplicationCollaboratorServiceMock.handleRequestCommand.willReturnAddCollaborator(AddCollaborator(
        actor,
        collaborator,
        existingCollaborators.map(_.emailAddress),
        LocalDateTime.now()
      ))
      EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.UpdateApplication.willReturnSuccess(productionApplication)

      val result: Application = await(service.updateApplication(productionApplication, request))

      result shouldBe productionApplication
    }

    "return UpstreamErrorResponse when call to third party developer fails" in new Setup {

      ApplicationCollaboratorServiceMock.handleRequestCommand.willReturnErrorsAddCollaborator()

      intercept[UpstreamErrorResponse] {
        await(service.updateApplication(productionApplication, request))
      }

    }

  }

  "removeCollaborator" should {
    val actor        = CollaboratorActor("someEMail")
    val collaborator = Collaborator("collaboratorEmail", DEVELOPER, Option(UserId.random))
    val request      = RemoveCollaboratorRequest(actor, collaborator.emailAddress, collaborator.role, LocalDateTime.now())
    "call third party application with decorated RemoveCollaborator when called" in new Setup {

      ApplicationCollaboratorServiceMock.handleRequestCommand.willReturnRemoveCollaborator(RemoveCollaborator(
        actor,
        collaborator,
        existingCollaborators.map(_.emailAddress),
        LocalDateTime.now()
      ))
      EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.UpdateApplication.willReturnSuccess(productionApplication)

      val result: Application = await(service.updateApplication(productionApplication, request))

      result shouldBe productionApplication
    }

    "return UpstreamErrorResponse when call to third party developer fails" in new Setup {

      ApplicationCollaboratorServiceMock.handleRequestCommand.willReturnErrorsRemoveCollaborator()

      intercept[UpstreamErrorResponse] {
        await(service.updateApplication(productionApplication, request))
      }

    }

  }

  "non request Type command" should {
    val actor        = CollaboratorActor("someEMail")
    val collaborator = Collaborator("collaboratorEmail", DEVELOPER, Option(UserId.random))

    "call third party application  with same command as passed in" in new Setup {
      val request = RemoveCollaborator(actor, collaborator, existingCollaborators.map(_.emailAddress), LocalDateTime.now())

      EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.UpdateApplication.willReturnSuccess(productionApplication)

      val result: Application = await(service.updateApplication(productionApplication, request))

      result shouldBe productionApplication
    }

  }
}
