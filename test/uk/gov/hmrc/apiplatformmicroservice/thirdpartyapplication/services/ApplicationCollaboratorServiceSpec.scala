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

import java.time.{Clock, Instant, LocalDateTime, ZoneOffset}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future.successful

import org.joda.time.DateTime
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.applications.domain.models.{Collaborator, Collaborators}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress
import uk.gov.hmrc.apiplatform.modules.developers.domain.models.UserId
import uk.gov.hmrc.apiplatformmicroservice.common.builder.{ApplicationBuilder, UserResponseBuilder}
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.Environment
import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors._
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.domain.{
  AddCollaboratorToTpaRequest,
  GetOrCreateUserIdRequest,
  GetOrCreateUserIdResponse,
  UnregisteredUserResponse,
  UserResponse
}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications._
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks._

class ApplicationCollaboratorServiceSpec extends AsyncHmrcSpec {

  implicit val hc = HeaderCarrier()

  trait Setup extends ThirdPartyApplicationConnectorModule with MockitoSugar
      with ArgumentMatchersSugar with UserResponseBuilder with ApplicationBuilder {

    val mockThirdPartyDeveloperConnector = mock[ThirdPartyDeveloperConnector]
    val fixedClock                       = Clock.fixed(Instant.now(), ZoneOffset.UTC)
    val service                          = new ApplicationCollaboratorService(EnvironmentAwareThirdPartyApplicationConnectorMock.instance, mockThirdPartyDeveloperConnector, fixedClock)

    val newCollaboratorEmail                    = LaxEmailAddress("newCollaborator@testuser.com")
    val newCollaboratorUserId                   = UserId.random
    val newCollaborator                         = Collaborator(newCollaboratorEmail, Collaborators.Roles.DEVELOPER, newCollaboratorUserId)
    val newCollaboratorUserResponse             = buildUserResponse(email = newCollaboratorEmail, userId = newCollaboratorUserId)
    val newCollaboratorUnregisteredUserResponse = UnregisteredUserResponse(newCollaboratorEmail, DateTime.now, newCollaboratorUserId)

    val requesterEmail            = LaxEmailAddress("adminRequester@example.com")
    val verifiedAdminEmail        = LaxEmailAddress("verifiedAdmin@example.com")
    val unverifiedAdminEmail      = LaxEmailAddress("unverifiedAdmin@example.com")
    val adminEmailsMinusRequester = Set(verifiedAdminEmail, unverifiedAdminEmail)
    val adminEmails               = Set(verifiedAdminEmail, unverifiedAdminEmail, requesterEmail)

    val adminMinusRequesterUserResponses =
      Seq(buildUserResponse(email = verifiedAdminEmail, userId = UserId.random), buildUserResponse(email = unverifiedAdminEmail, verified = false, userId = UserId.random))

    val adminUserResponses = Seq(
      buildUserResponse(email = verifiedAdminEmail, userId = UserId.random),
      buildUserResponse(email = unverifiedAdminEmail, verified = false, userId = UserId.random),
      buildUserResponse(email = requesterEmail, userId = UserId.random)
    )

    val productionApplication = buildApplication().deployedToProduction.withCollaborators(Set(
      Collaborator(LaxEmailAddress("collaborator1@example.com"), Collaborators.Roles.DEVELOPER, UserId.random),
      Collaborator(verifiedAdminEmail, Collaborators.Roles.ADMINISTRATOR, UserId.random),
      Collaborator(unverifiedAdminEmail, Collaborators.Roles.ADMINISTRATOR, UserId.random),
      Collaborator(requesterEmail, Collaborators.Roles.ADMINISTRATOR, UserId.random)
    ))

    val addCollaboratorToTpaRequestWithRequesterEmail    = AddCollaboratorToTpaRequest(requesterEmail, newCollaborator, true, Set(verifiedAdminEmail))
    val addCollaboratorToTpaRequestWithoutRequesterEmail = AddCollaboratorToTpaRequest(LaxEmailAddress(""), newCollaborator, true, Set(verifiedAdminEmail, requesterEmail))

    val addCollaboratorSuccessResult           = AddCollaboratorSuccessResult(userRegistered = true)
    val collaboratorAlreadyExistsFailureResult = CollaboratorAlreadyExistsFailureResult

    val getOrCreateUserIdRequest  = GetOrCreateUserIdRequest(newCollaboratorEmail)
    val getOrCreateUserIdResponse = GetOrCreateUserIdResponse(newCollaboratorUserId)
  }

  "addCollaborator" should {
    "create unregistered user when developer is not registered on principal app" in new Setup {
      when(mockThirdPartyDeveloperConnector.fetchByEmails(eqTo(adminEmailsMinusRequester))(*)).thenReturn(successful(adminMinusRequesterUserResponses))
      when(mockThirdPartyDeveloperConnector.getOrCreateUserId(eqTo(GetOrCreateUserIdRequest(newCollaboratorEmail)))(*)).thenReturn(successful(getOrCreateUserIdResponse))

      EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.AddCollaborator.willReturnSuccess

      await(service.addCollaborator(productionApplication, newCollaboratorEmail, Collaborators.Roles.DEVELOPER, Some(requesterEmail))) shouldBe addCollaboratorSuccessResult

      EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.AddCollaborator.verifyCalled(1, productionApplication.id, addCollaboratorToTpaRequestWithRequesterEmail)
    }

    "create unregistered user when developer is not registered on subordinate app" in new Setup {
      when(mockThirdPartyDeveloperConnector.fetchByEmails(eqTo(adminEmailsMinusRequester))(*)).thenReturn(successful(adminMinusRequesterUserResponses))
      when(mockThirdPartyDeveloperConnector.getOrCreateUserId(eqTo(GetOrCreateUserIdRequest(newCollaboratorEmail)))(*)).thenReturn(successful(getOrCreateUserIdResponse))

      EnvironmentAwareThirdPartyApplicationConnectorMock.Subordinate.AddCollaborator.willReturnSuccess

      await(
        service.addCollaborator(productionApplication.copy(deployedTo = Environment.SANDBOX), newCollaboratorEmail, Collaborators.Roles.DEVELOPER, Some(requesterEmail))
      ) shouldBe addCollaboratorSuccessResult

      EnvironmentAwareThirdPartyApplicationConnectorMock.Subordinate.AddCollaborator.verifyCalled(1, productionApplication.id, addCollaboratorToTpaRequestWithRequesterEmail)
    }

    "production app with requester email which shouldn't get notification email" in new Setup {
      when(mockThirdPartyDeveloperConnector.fetchByEmails(eqTo(adminEmailsMinusRequester))(*)).thenReturn(successful(adminMinusRequesterUserResponses))
      when(mockThirdPartyDeveloperConnector.getOrCreateUserId(eqTo(GetOrCreateUserIdRequest(newCollaboratorEmail)))(*)).thenReturn(successful(getOrCreateUserIdResponse))

      EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.AddCollaborator.willReturnSuccess

      await(service.addCollaborator(productionApplication, newCollaboratorEmail, Collaborators.Roles.DEVELOPER, Some(requesterEmail))) shouldBe addCollaboratorSuccessResult

      EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.AddCollaborator.verifyCalled(1, productionApplication.id, addCollaboratorToTpaRequestWithRequesterEmail)
    }

    "include correct set of admins to email when no requester email is specified (GK case)" in new Setup {
      when(mockThirdPartyDeveloperConnector.fetchByEmails(eqTo(adminEmails))(*)).thenReturn(successful(adminUserResponses))
      when(mockThirdPartyDeveloperConnector.getOrCreateUserId(*)(*)).thenReturn(successful(getOrCreateUserIdResponse))

      EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.AddCollaborator.willReturnSuccess

      await(service.addCollaborator(productionApplication, newCollaboratorEmail, Collaborators.Roles.DEVELOPER, None)) shouldBe addCollaboratorSuccessResult

      EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.AddCollaborator.verifyCalled(1, productionApplication.id, addCollaboratorToTpaRequestWithoutRequesterEmail)
    }

    "propagate TeamMemberAlreadyExists from connector in production app" in new Setup {
      when(mockThirdPartyDeveloperConnector.fetchByEmails(eqTo(adminEmailsMinusRequester))(*)).thenReturn(successful(adminMinusRequesterUserResponses))
      when(mockThirdPartyDeveloperConnector.getOrCreateUserId(eqTo(GetOrCreateUserIdRequest(newCollaboratorEmail)))(*)).thenReturn(successful(getOrCreateUserIdResponse))

      EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.AddCollaborator.willReturnFailure

      await(
        service.addCollaborator(productionApplication, newCollaboratorEmail, Collaborators.Roles.DEVELOPER, Some(requesterEmail))
      ) shouldBe collaboratorAlreadyExistsFailureResult

      EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.AddCollaborator.verifyCalled(1, productionApplication.id, addCollaboratorToTpaRequestWithRequesterEmail)
    }
  }

  "handleRequest" should {
    val actor = CollaboratorActor("someEMail")

    "decorate RemoveCollaborator Request when third party developer call is successful" in new Setup {
      val userResponse: Seq[UserResponse] = adminMinusRequesterUserResponses
      val collaborator                    = Collaborator(LaxEmailAddress("collaboratorEmail"), Collaborators.Roles.DEVELOPER, getOrCreateUserIdResponse.userId)
      val request                         = RemoveCollaboratorRequest(actor, collaborator.emailAddress, Collaborators.Roles.DEVELOPER, LocalDateTime.now(fixedClock))

      when(mockThirdPartyDeveloperConnector.getOrCreateUserId(*)(*)).thenReturn(successful(getOrCreateUserIdResponse))

      when(mockThirdPartyDeveloperConnector.fetchByEmails(*)(*)).thenReturn(successful(userResponse))

      val response = await(service.handleRequestCommand(productionApplication, request))
      response shouldBe RemoveCollaborator(request.actor, collaborator, Set(verifiedAdminEmail), LocalDateTime.now(fixedClock))
    }

    "decorate AddCollaborator Request when third party developer call is successful" in new Setup {
      val userResponse: Seq[UserResponse] = adminMinusRequesterUserResponses
      val collaborator                    = Collaborator(LaxEmailAddress("collaboratorEmail"), Collaborators.Roles.DEVELOPER, getOrCreateUserIdResponse.userId)
      val request                         = AddCollaboratorRequest(actor, collaborator.emailAddress, Collaborators.Roles.DEVELOPER, LocalDateTime.now(fixedClock))

      when(mockThirdPartyDeveloperConnector.getOrCreateUserId(*)(*)).thenReturn(successful(getOrCreateUserIdResponse))

      when(mockThirdPartyDeveloperConnector.fetchByEmails(*)(*)).thenReturn(successful(userResponse))

      val response = await(service.handleRequestCommand(productionApplication, request))
      response shouldBe AddCollaborator(request.actor, collaborator, Set(verifiedAdminEmail), LocalDateTime.now(fixedClock))
    }
  }
}
