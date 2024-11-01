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

package uk.gov.hmrc.apiplatformmicroservice.common.builder

import java.time.Instant

import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, ClientId, Environment, LaxEmailAddress}
import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock
import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.Access
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models._

trait ApplicationBuilder extends CollaboratorsBuilder with FixedClock with ApplicationWithCollaboratorsFixtures {

  def buildSandboxApp(
    ): ApplicationWithCollaborators = {

    standardApp.inSandbox()
  }

  val DefaultApplication = buildSandboxApp()

  implicit class ApplicationStateExtension(applicationState: ApplicationState) {
    def inProduction        = applicationState.copy(name = State.PRODUCTION)
    def inTesting           = applicationState.copy(name = State.TESTING)
    def pendingGKApproval   = applicationState.copy(name = State.PENDING_GATEKEEPER_APPROVAL)
    def pendingVerification = applicationState.copy(name = State.PENDING_REQUESTER_VERIFICATION)
  }

  implicit class ApplicationExtension(app: ApplicationWithCollaborators) {
    def deployedToProduction = app.modify(_.copy(deployedTo = Environment.PRODUCTION))
    def deployedToSandbox    = app.modify(_.copy(deployedTo = Environment.SANDBOX))

    def withoutCollaborator(email: LaxEmailAddress)         = app.copy(collaborators = app.collaborators.filterNot(c => c.emailAddress == email))
    def withCollaborators(collaborators: Set[Collaborator]) = app.copy(collaborators = collaborators)

    def withId(id: ApplicationId)        = app.modify(_.copy(id = id))
    def withClientId(clientId: ClientId) = app.modify(_.copy(clientId = clientId))
    def withGatewayId(gatewayId: String) = app.modify(_.copy(gatewayId = gatewayId))

    def withName(name: String)               = app.modify(_.copy(name = ApplicationName(name)))
    def withDescription(description: String) = app.modify(_.copy(description = Some(description)))

    def withAdmin(email: LaxEmailAddress) = {
      val app1 = app.withoutCollaborator(email)
      app1.copy(collaborators = app1.collaborators + buildCollaborator(email, Collaborator.Roles.ADMINISTRATOR))
    }

    def withDeveloper(email: LaxEmailAddress) = {
      val app1 = app.withoutCollaborator(email)
      app1.copy(collaborators = app1.collaborators + buildCollaborator(email, Collaborator.Roles.DEVELOPER))
    }

    def asStandard   = app.withAccess(Access.Standard())
    def asPrivileged = app.withAccess(Access.Privileged())
    def asROPC       = app.withAccess(Access.Ropc())

    def inProduction        = app.modifyState(_.inProduction)
    def inTesting           = app.modifyState(_.inTesting)
    def pendingGKApproval   = app.modifyState(_.pendingGKApproval)
    def pendingVerification = app.modifyState(_.pendingVerification)

    def withBlocked(isBlocked: Boolean) = app.modify(_.copy(blocked = isBlocked))
    def blocked                         = app.modify(_.copy(blocked = true))
    def unblocked                       = app.modify(_.copy(blocked = false))

    def withCheckInformation(checkInfo: CheckInformation) = app.modify(_.copy(checkInformation = Some(checkInfo)))
    def withEmptyCheckInformation                         = app.modify(_.copy(checkInformation = Some(CheckInformation())))
    def noCheckInformation                                = app.modify(_.copy(checkInformation = None))

    def allowIPs(ips: String*) = app.modify(_.copy(ipAllowlist = app.details.ipAllowlist.copy(allowlist = app.details.ipAllowlist.allowlist ++ ips)))

    def withCreatedOn(createdOnDate: Instant)   = app.modify(_.copy(createdOn = createdOnDate))
    def withLastAccess(lastAccessDate: Instant) = app.modify(_.copy(lastAccess = Some(lastAccessDate)))
  }
}
