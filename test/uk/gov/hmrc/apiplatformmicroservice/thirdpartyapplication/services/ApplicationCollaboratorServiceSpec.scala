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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.http.Status.{CONFLICT, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.{ApplicationId, Environment}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors._
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.{Application, ApplicationWithSubscriptionData, ClientId, Collaborator, Role}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks._
import uk.gov.hmrc.apiplatformmicroservice.util.AsyncHmrcSpec
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, NotFoundException, Upstream4xxResponse}
import uk.gov.hmrc.time.DateTimeUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Future.failed

class ApplicationCollaboratorServiceSpec extends AsyncHmrcSpec {

  implicit val hc = HeaderCarrier()

  val id: ApplicationId = ApplicationId("one")
  val clientId: ClientId = ClientId("123")
  val application: Application = Application(id, clientId, "gatewayId", "name", DateTimeUtils.now, DateTimeUtils.now, None, Environment.SANDBOX, Some("description"))
  val BANG = new RuntimeException("BANG")

  trait Setup extends ThirdPartyApplicationConnectorModule with MockitoSugar with ArgumentMatchersSugar {
    val service = new ApplicationCollaboratorService(EnvironmentAwareThirdPartyApplicationConnectorMock.instance)
    val email = "email@testuser.com"
    val teamMember = Collaborator(email, Role.ADMINISTRATOR)
//    val developer = Developer(teamMember.emailAddress, "name", "surname")
    val adminEmail = "admin.email@example.com"
    val adminsToEmail = Set.empty[String]
//    val request = AddTeamMemberRequest(adminEmail, teamMember, isRegistered = true, adminsToEmail)
  }

    "add teamMember" should {


//      "add teamMember successfully in production app" in new Setup {
//        private val response = AddTeamMemberResponse(registeredUser = true)
//
//        when(mockDeveloperConnector.fetchDeveloper(email)).thenReturn(successful(Some(developer)))
//        when(mockDeveloperConnector.fetchByEmails(*)(*)).thenReturn(successful(Seq.empty))
//        theProductionConnectorthenReturnTheApplication(productionApplicationId, productionApplication)
//        when(mockProductionApplicationConnector.addTeamMember(productionApplicationId, request)).thenReturn(successful(response))
//
//        await(applicationService.addTeamMember(productionApplication, adminEmail, teamMember)) shouldBe response
//      }
//
//      "create unregistered user when developer is not registered" in new Setup {
//        when(mockDeveloperConnector.fetchDeveloper(email)).thenReturn(successful(None))
//        when(mockDeveloperConnector.fetchByEmails(*)(*)).thenReturn(successful(Seq.empty))
//        theProductionConnectorthenReturnTheApplication(productionApplicationId, productionApplication)
//        when(mockProductionApplicationConnector.addTeamMember(productionApplicationId, request.copy(isRegistered = false)))
//          .thenReturn(successful(AddTeamMemberResponse(registeredUser = false)))
//        when(mockDeveloperConnector.createUnregisteredUser(teamMember.emailAddress)).thenReturn(successful(OK))
//
//        await(applicationService.addTeamMember(productionApplication, adminEmail, teamMember))
//
//        verify(mockDeveloperConnector, times(1)).createUnregisteredUser(eqTo(teamMember.emailAddress))(*)
//      }
//
//      "not create unregistered user when developer is already registered" in new Setup {
//        when(mockDeveloperConnector.fetchDeveloper(email)).thenReturn(successful(Some(Developer(teamMember.emailAddress, "name", "surname"))))
//        when(mockDeveloperConnector.fetchByEmails(*)(*)).thenReturn(successful(Seq.empty))
//        theProductionConnectorthenReturnTheApplication(productionApplicationId, productionApplication)
//        when(mockProductionApplicationConnector.addTeamMember(productionApplicationId, request))
//          .thenReturn(successful(AddTeamMemberResponse(registeredUser = true)))
//
//        await(applicationService.addTeamMember(productionApplication, adminEmail, teamMember))
//
//        verify(mockDeveloperConnector, times(0)).createUnregisteredUser(eqTo(teamMember.emailAddress))(*)
//      }
//
//      "propagate TeamMemberAlreadyExists from connector in production app" in new Setup {
//        when(mockDeveloperConnector.fetchDeveloper(email)).thenReturn(successful(Some(developer)))
//        when(mockDeveloperConnector.fetchByEmails(*)(*)).thenReturn(successful(Seq.empty))
//        theProductionConnectorthenReturnTheApplication(productionApplicationId, productionApplication)
//        when(mockProductionApplicationConnector.addTeamMember(productionApplicationId, request))
//          .thenReturn(failed(new TeamMemberAlreadyExists))
//
//        intercept[TeamMemberAlreadyExists] {
//          await(applicationService.addTeamMember(productionApplication, adminEmail, teamMember))
//        }
//      }
//
//      "propagate ApplicationNotFound from connector in production app" in new Setup {
//        when(mockDeveloperConnector.fetchDeveloper(email)).thenReturn(successful(Some(developer)))
//        when(mockDeveloperConnector.fetchByEmails(*)(*)).thenReturn(successful(Seq.empty))
//        theProductionConnectorthenReturnTheApplication(productionApplicationId, productionApplication)
//        when(mockProductionApplicationConnector.addTeamMember(productionApplicationId, request))
//          .thenReturn(failed(new ApplicationAlreadyExists))
//        intercept[ApplicationAlreadyExists] {
//          await(applicationService.addTeamMember(productionApplication, adminEmail, teamMember))
//        }
//      }
//      "add teamMember successfully in sandbox app" in new Setup {
//        private val response = AddTeamMemberResponse(registeredUser = true)
//
//        when(mockDeveloperConnector.fetchDeveloper(email)).thenReturn(successful(Some(developer)))
//        when(mockDeveloperConnector.fetchByEmails(*)(*)).thenReturn(successful(Seq.empty))
//        theSandboxConnectorthenReturnTheApplication(sandboxApplicationId, sandboxApplication)
//        when(mockSandboxApplicationConnector.addTeamMember(sandboxApplicationId, request)).thenReturn(successful(response))
//
//        await(applicationService.addTeamMember(sandboxApplication, adminEmail, teamMember)) shouldBe response
//      }
//
//      "propagate TeamMemberAlreadyExists from connector in sandbox app" in new Setup {
//        when(mockDeveloperConnector.fetchDeveloper(email)).thenReturn(successful(Some(developer)))
//        when(mockDeveloperConnector.fetchByEmails(*)(*)).thenReturn(successful(Seq.empty))
//        theSandboxConnectorthenReturnTheApplication(sandboxApplicationId, sandboxApplication)
//        when(mockSandboxApplicationConnector.addTeamMember(sandboxApplicationId, request))
//          .thenReturn(failed(new TeamMemberAlreadyExists))
//        intercept[TeamMemberAlreadyExists] {
//          await(applicationService.addTeamMember(sandboxApplication, adminEmail, teamMember))
//        }
//      }
//
//      "propagate ApplicationNotFound from connector in sandbox app" in new Setup {
//        when(mockDeveloperConnector.fetchDeveloper(email)).thenReturn(successful(Some(developer)))
//        when(mockDeveloperConnector.fetchByEmails(*)(*)).thenReturn(successful(Seq.empty))
//        theSandboxConnectorthenReturnTheApplication(sandboxApplicationId, sandboxApplication)
//        when(mockSandboxApplicationConnector.addTeamMember(sandboxApplicationId, request))
//          .thenReturn(failed(new ApplicationAlreadyExists))
//        intercept[ApplicationAlreadyExists] {
//          await(applicationService.addTeamMember(sandboxApplication, adminEmail, teamMember))
//        }
//      }
//
//      "include correct set of admins to email" in new Setup {
//        private val verifiedAdmin = Collaborator("verified@example.com", Role.ADMINISTRATOR)
//        private val unverifiedAdmin = Collaborator("unverified@example.com", Role.ADMINISTRATOR)
//        private val adderAdmin = Collaborator(adminEmail, Role.ADMINISTRATOR)
//        private val verifiedDeveloper = Collaborator("developer@example.com", Role.DEVELOPER)
//        private val nonAdderAdmins = Seq(User("verified@example.com", Some(true)), User("unverified@example.com", Some(false)))
//
//        private val application = productionApplication.copy(collaborators = Set(verifiedAdmin, unverifiedAdmin, adderAdmin, verifiedDeveloper))
//
//        private val response = AddTeamMemberResponse(registeredUser = true)
//
//        when(mockDeveloperConnector.fetchDeveloper(email)).thenReturn(successful(Some(developer)))
//        when(mockDeveloperConnector.fetchByEmails(eqTo(Set("verified@example.com", "unverified@example.com")))(*))
//          .thenReturn(successful(nonAdderAdmins))
//        theProductionConnectorthenReturnTheApplication(productionApplicationId, application)
//        when(mockProductionApplicationConnector.addTeamMember(*[ApplicationId], *)(*)).thenReturn(successful(response))
//
//        await(applicationService.addTeamMember(application, adderAdmin.emailAddress, teamMember)) shouldBe response
//        verify(mockProductionApplicationConnector).addTeamMember(eqTo(productionApplicationId), eqTo(request.copy(adminsToEmail = Set("verified@example.com"))))(*)
//      }
//    }
  }
}
