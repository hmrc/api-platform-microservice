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

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, ClientId, Environment, LaxEmailAddress}
import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock
import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.Access
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.Application

trait ApplicationBuilder extends CollaboratorsBuilder with FixedClock {

  def buildApplication(
      appId: ApplicationId = ApplicationId.random,
      createdOn: Instant = instant,
      lastAccess: Instant = instant,
      checkInformation: Option[CheckInformation] = None
    ): Application = {
    val clientId                 = ClientId.random
    val appOwnerEmail            = "a@b.com".toLaxEmail
    val grantLength: GrantLength = GrantLength.EIGHTEEN_MONTHS

    Application(
      id = appId,
      clientId = clientId,
      gatewayId = "",
      name = s"${appId.value}-name",
      createdOn = createdOn,
      lastAccess = Some(lastAccess),
      grantLength = grantLength,
      lastAccessTokenUsage = None,
      deployedTo = Environment.SANDBOX,
      description = Some(s"$appId-description"),
      collaborators = buildCollaborators(Seq((appOwnerEmail, Collaborator.Roles.ADMINISTRATOR))),
      access = Access.Standard(
        redirectUris = List("https://red1", "https://red2").map(RedirectUri.unsafeApply),
        termsAndConditionsUrl = Some("http://tnc-url.com")
      ),
      state = ApplicationState(State.PRODUCTION, None, None, None, instant),
      rateLimitTier = RateLimitTier.BRONZE,
      blocked = false,
      checkInformation = checkInformation,
      ipAllowlist = IpAllowlist(),
      moreApplication = MoreApplication(true)
    )
  }

  val DefaultApplication = buildApplication()

  implicit class ApplicationStateExtension(applicationState: ApplicationState) {
    def inProduction        = applicationState.copy(name = State.PRODUCTION)
    def inTesting           = applicationState.copy(name = State.TESTING)
    def pendingGKApproval   = applicationState.copy(name = State.PENDING_GATEKEEPER_APPROVAL)
    def pendingVerification = applicationState.copy(name = State.PENDING_REQUESTER_VERIFICATION)
  }

  implicit class ApplicationExtension(app: Application) {
    def deployedToProduction = app.copy(deployedTo = Environment.PRODUCTION)
    def deployedToSandbox    = app.copy(deployedTo = Environment.SANDBOX)

    def withoutCollaborator(email: LaxEmailAddress)         = app.copy(collaborators = app.collaborators.filterNot(c => c.emailAddress == email))
    def withCollaborators(collaborators: Set[Collaborator]) = app.copy(collaborators = collaborators)

    def withId(id: ApplicationId)        = app.copy(id = id)
    def withClientId(clientId: ClientId) = app.copy(clientId = clientId)
    def withGatewayId(gatewayId: String) = app.copy(gatewayId = gatewayId)

    def withName(name: String)               = app.copy(name = name)
    def withDescription(description: String) = app.copy(description = Some(description))

    def withAdmin(email: LaxEmailAddress) = {
      val app1 = app.withoutCollaborator(email)
      app1.copy(collaborators = app1.collaborators + buildCollaborator(email, Collaborator.Roles.ADMINISTRATOR))
    }

    def withDeveloper(email: LaxEmailAddress) = {
      val app1 = app.withoutCollaborator(email)
      app1.copy(collaborators = app1.collaborators + buildCollaborator(email, Collaborator.Roles.DEVELOPER))
    }

    def withAccess(access: Access) = app.copy(access = access)
    def asStandard                 = app.copy(access = Access.Standard())
    def asPrivileged               = app.copy(access = Access.Privileged())
    def asROPC                     = app.copy(access = Access.Ropc())

    def withState(state: ApplicationState) = app.copy(state = state)
    def inProduction                       = app.copy(state = app.state.inProduction)
    def inTesting                          = app.copy(state = app.state.inTesting)
    def pendingGKApproval                  = app.copy(state = app.state.pendingGKApproval)
    def pendingVerification                = app.copy(state = app.state.pendingVerification)

    def withBlocked(isBlocked: Boolean) = app.copy(blocked = isBlocked)
    def blocked                         = app.copy(blocked = true)
    def unblocked                       = app.copy(blocked = false)

    def withCheckInformation(checkInfo: CheckInformation) = app.copy(checkInformation = Some(checkInfo))
    def withEmptyCheckInformation                         = app.copy(checkInformation = Some(CheckInformation()))
    def noCheckInformation                                = app.copy(checkInformation = None)

    def allowIPs(ips: String*) = app.copy(ipAllowlist = app.ipAllowlist.copy(allowlist = app.ipAllowlist.allowlist ++ ips))

    def withCreatedOn(createdOnDate: Instant)   = app.copy(createdOn = createdOnDate)
    def withLastAccess(lastAccessDate: Instant) = app.copy(lastAccess = Some(lastAccessDate))
  }
}
