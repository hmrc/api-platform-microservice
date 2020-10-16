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

package uk.gov.hmrc.apiplatformmicroservice.common.builder

import uk.gov.hmrc.time.DateTimeUtils
import org.joda.time.DateTime
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.CheckInformation
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.Application
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.ClientId
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.Environment
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.ApplicationState
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.Collaborator
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.Access
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.Standard
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.Privileged
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.Role
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.ApplicationState
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.State
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.ROPC

trait ApplicationBuilder extends CollaboratorsBuilder {
  def buildApplication(appId: ApplicationId = ApplicationId.random, createdOn: DateTime = DateTimeUtils.now, lastAccess: DateTime = DateTimeUtils.now, checkInformation: Option[CheckInformation] = None): Application = {
    val clientId = ClientId.random
    val appOwnerEmail = "a@b.com"

    Application(
      id = appId,
      clientId = clientId,
      gatewayId = "",
      name = s"$appId-name",
      createdOn = createdOn,
      lastAccess = lastAccess,
      lastAccessTokenUsage = None,
      deployedTo = Environment.SANDBOX,
      description = Some(s"$appId-description"),
      collaborators = buildCollaborators(Seq((appOwnerEmail, Role.ADMINISTRATOR))),
      access = Standard(
        redirectUris = Seq("https://red1", "https://red2"),
        termsAndConditionsUrl = Some("http://tnc-url.com")
      ),
      state = ApplicationState(State.PRODUCTION, None, None),
      rateLimitTier = "BRONZE",
      blocked = false,
      checkInformation = checkInformation
    )
  }

  val DefaultApplication = buildApplication()

  implicit class ApplicationStateExtension(applicationState: ApplicationState) {
    def inProduction = applicationState.copy(name = State.PRODUCTION)
    def inTesting = applicationState.copy(name = State.TESTING)
    def pendingGKApproval = applicationState.copy(name = State.PENDING_GATEKEEPER_APPROVAL)
    def pendingVerification = applicationState.copy(name = State.PENDING_REQUESTER_VERIFICATION)
  }

  implicit class ApplicationExtension(app: Application) {
    def deployedToProduction = app.copy(deployedTo = Environment.PRODUCTION)
    def deployedToSandbox = app.copy(deployedTo = Environment.SANDBOX)

    def withoutCollaborator(email: String) = app.copy(collaborators = app.collaborators.filterNot(c => c.emailAddress == email))
    def withCollaborators(collaborators: Set[Collaborator]) = app.copy(collaborators = collaborators)

    def withId(id: ApplicationId) = app.copy(id = id)
    def withClientId(clientId: ClientId) = app.copy(clientId = clientId)
    def withGatewayId(gatewayId: String) = app.copy(gatewayId = gatewayId)
    
    def withName(name: String) = app.copy(name = name)
    def withDescription(description: String) = app.copy(description = Some(description))

    def withAdmin(email: String) = {
      val app1 = app.withoutCollaborator(email)
      app1.copy(collaborators = app1.collaborators + Collaborator(email, Role.ADMINISTRATOR))
    }
    def withDeveloper(email: String) = {
      val app1 = app.withoutCollaborator(email)
      app1.copy(collaborators = app1.collaborators + Collaborator(email, Role.DEVELOPER))
    }

    def withAccess(access: Access) = app.copy(access = access)
    def asStandard = app.copy(access = Standard())
    def asPrivileged = app.copy(access = Privileged())
    def asROPC = app.copy(access = ROPC())

    def withState(state: ApplicationState) = app.copy(state = state)
    def inProduction = app.copy(state = app.state.inProduction)
    def inTesting = app.copy(state = app.state.inTesting)
    def pendingGKApproval = app.copy(state = app.state.pendingGKApproval)
    def pendingVerification = app.copy(state = app.state.pendingVerification)

    def withBlocked(isBlocked: Boolean) = app.copy(blocked = isBlocked)
    def blocked = app.copy(blocked = true)
    def unblocked = app.copy(blocked = false)

    def withCheckInformation(checkInfo: CheckInformation) = app.copy(checkInformation = Some(checkInfo))
    def withEmptyCheckInformation = app.copy(checkInformation = Some(CheckInformation()))
    def noCheckInformation = app.copy(checkInformation = None)

    def allowIPs(ips: String*) = app.copy(ipWhitelist = app.ipWhitelist ++ ips)

    def withCreatedOn(createdOnDate: DateTime) = app.copy(createdOn = createdOnDate)
    def withLastAccess(lastAccessDate: DateTime) = app.copy(lastAccess = lastAccessDate)
  }  
}
