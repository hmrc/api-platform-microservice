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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services

import org.joda.time.DateTime
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import uk.gov.hmrc.apiplatformmicroservice.common.builder.{ApplicationBuilder, UserResponseBuilder}
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.{Environment, UserId}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors._
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.domain.{AddCollaboratorToTpaRequest, GetOrCreateUserIdRequest, GetOrCreateUserIdResponse, UnregisteredUserResponse}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.{Collaborator, Role}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks._
import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future.successful

class ApplicationCollaboratorServiceSpec extends AsyncHmrcSpec {

  implicit val hc = HeaderCarrier()

  trait Setup extends ThirdPartyApplicationConnectorModule with MockitoSugar
    with ArgumentMatchersSugar with UserResponseBuilder with ApplicationBuilder {

    val mockThirdPartyDeveloperConnector = mock[ThirdPartyDeveloperConnector]
    val service = new ApplicationCollaboratorService(EnvironmentAwareThirdPartyApplicationConnectorMock.instance, mockThirdPartyDeveloperConnector)

    val newCollaboratorEmail = "newCollaborator@testuser.com"
    val newCollaboratorUserId = UserId.random
    val newCollaborator = Collaborator(newCollaboratorEmail, Role.DEVELOPER, Some(newCollaboratorUserId))
    val newCollaboratorUserResponse = buildUserResponse(email = newCollaboratorEmail, userId = newCollaboratorUserId)
    val newCollaboratorUnregisteredUserResponse = UnregisteredUserResponse(newCollaboratorEmail, DateTime.now, newCollaboratorUserId)

    val requesterEmail = "adminRequester@example.com"
    val verifiedAdminEmail = "verifiedAdmin@example.com"
    val unverifiedAdminEmail = "unverifiedAdmin@example.com"
    val adminEmailsMinusRequester = Set(verifiedAdminEmail, unverifiedAdminEmail)
    val adminEmails = Set(verifiedAdminEmail, unverifiedAdminEmail, requesterEmail)
    val adminMinusRequesterUserResponses = Seq(buildUserResponse(email = verifiedAdminEmail, userId = UserId.random), buildUserResponse(email = unverifiedAdminEmail, verified = false, userId = UserId.random))
    val adminUserResponses = Seq(buildUserResponse(email = verifiedAdminEmail, userId = UserId.random),
      buildUserResponse(email = unverifiedAdminEmail, verified = false, userId = UserId.random),
      buildUserResponse(email = requesterEmail, userId = UserId.random)
    )

    val productionApplication = buildApplication().deployedToProduction.withCollaborators(Set(
      Collaborator("collaborator1@example.com", Role.DEVELOPER, None),
      Collaborator(verifiedAdminEmail, Role.ADMINISTRATOR, None),
      Collaborator(unverifiedAdminEmail, Role.ADMINISTRATOR, None),
      Collaborator(requesterEmail, Role.ADMINISTRATOR, None)))

    val addCollaboratorToTpaRequestWithRequesterEmail = AddCollaboratorToTpaRequest(requesterEmail, newCollaborator, true, Set(verifiedAdminEmail))
    val addCollaboratorToTpaRequestWithoutRequesterEmail = AddCollaboratorToTpaRequest("", newCollaborator, true, Set(verifiedAdminEmail, requesterEmail))

    val addCollaboratorSuccessResult = AddCollaboratorSuccessResult(userRegistered = true)
    val collaboratorAlreadyExistsFailureResult = CollaboratorAlreadyExistsFailureResult

    val getOrCreateUserIdRequest = GetOrCreateUserIdRequest(newCollaboratorEmail)
    val getOrCreateUserIdResponse = GetOrCreateUserIdResponse(newCollaboratorUserId)
  }

    "addCollaborator" should {
      "create unregistered user when developer is not registered on principal app" in new Setup {
        when(mockThirdPartyDeveloperConnector.fetchByEmails(eqTo(adminEmailsMinusRequester))(*)).thenReturn(successful(adminMinusRequesterUserResponses))
        when(mockThirdPartyDeveloperConnector.getOrCreateUserId(eqTo(GetOrCreateUserIdRequest(newCollaboratorEmail)))(*)).thenReturn(successful(getOrCreateUserIdResponse))

        EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.AddCollaborator.willReturnSuccess

        await(service.addCollaborator(productionApplication, newCollaboratorEmail, Role.DEVELOPER, Some(requesterEmail))) shouldBe addCollaboratorSuccessResult

        EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.AddCollaborator.verifyCalled(1, productionApplication.id, addCollaboratorToTpaRequestWithRequesterEmail)
      }

      "create unregistered user when developer is not registered on subordinate app" in new Setup {
        when(mockThirdPartyDeveloperConnector.fetchByEmails(eqTo(adminEmailsMinusRequester))(*)).thenReturn(successful(adminMinusRequesterUserResponses))
        when(mockThirdPartyDeveloperConnector.getOrCreateUserId(eqTo(GetOrCreateUserIdRequest(newCollaboratorEmail)))(*)).thenReturn(successful(getOrCreateUserIdResponse))

        EnvironmentAwareThirdPartyApplicationConnectorMock.Subordinate.AddCollaborator.willReturnSuccess

        await(service.addCollaborator(productionApplication.copy(deployedTo = Environment.SANDBOX), newCollaboratorEmail, Role.DEVELOPER, Some(requesterEmail))) shouldBe addCollaboratorSuccessResult

        EnvironmentAwareThirdPartyApplicationConnectorMock.Subordinate.AddCollaborator.verifyCalled(1, productionApplication.id, addCollaboratorToTpaRequestWithRequesterEmail)
      }

      "production app with requester email which shouldn't get notification email" in new Setup {
        when(mockThirdPartyDeveloperConnector.fetchByEmails(eqTo(adminEmailsMinusRequester))(*)).thenReturn(successful(adminMinusRequesterUserResponses))
        when(mockThirdPartyDeveloperConnector.getOrCreateUserId(eqTo(GetOrCreateUserIdRequest(newCollaboratorEmail)))(*)).thenReturn(successful(getOrCreateUserIdResponse))

        EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.AddCollaborator.willReturnSuccess

        await(service.addCollaborator(productionApplication, newCollaboratorEmail, Role.DEVELOPER, Some(requesterEmail))) shouldBe addCollaboratorSuccessResult

        EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.AddCollaborator.verifyCalled(1, productionApplication.id, addCollaboratorToTpaRequestWithRequesterEmail)
      }

      "include correct set of admins to email when no requester email is specified (GK case)" in new Setup {
        when(mockThirdPartyDeveloperConnector.fetchByEmails(eqTo(adminEmails))(*)).thenReturn(successful(adminUserResponses))
        when(mockThirdPartyDeveloperConnector.getOrCreateUserId(*)(*)).thenReturn(successful(getOrCreateUserIdResponse))

        EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.AddCollaborator.willReturnSuccess

        await(service.addCollaborator(productionApplication, newCollaboratorEmail, Role.DEVELOPER, None)) shouldBe addCollaboratorSuccessResult

        EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.AddCollaborator.verifyCalled(1, productionApplication.id, addCollaboratorToTpaRequestWithoutRequesterEmail)
      }

      "propagate TeamMemberAlreadyExists from connector in production app" in new Setup {
        when(mockThirdPartyDeveloperConnector.fetchByEmails(eqTo(adminEmailsMinusRequester))(*)).thenReturn(successful(adminMinusRequesterUserResponses))
        when(mockThirdPartyDeveloperConnector.getOrCreateUserId(eqTo(GetOrCreateUserIdRequest(newCollaboratorEmail)))(*)).thenReturn(successful(getOrCreateUserIdResponse))

        EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.AddCollaborator.willReturnFailure

        await(service.addCollaborator(productionApplication, newCollaboratorEmail, Role.DEVELOPER, Some(requesterEmail))) shouldBe collaboratorAlreadyExistsFailureResult

        EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.AddCollaborator.verifyCalled(1, productionApplication.id, addCollaboratorToTpaRequestWithRequesterEmail)
      }
  }
}
