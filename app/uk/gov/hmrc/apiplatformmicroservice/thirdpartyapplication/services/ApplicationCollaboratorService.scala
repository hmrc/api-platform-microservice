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

import java.time.{Clock, LocalDateTime}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.developers.domain.models.UserId
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.domain.{AddCollaboratorToTpaRequest, GetOrCreateUserIdRequest}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.{AddCollaboratorResult, EnvironmentAwareThirdPartyApplicationConnector, ThirdPartyDeveloperConnector}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications._
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.{Collaborator, Collaborators}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress

@Singleton
class ApplicationCollaboratorService @Inject() (
    thirdPartyApplicationConnector: EnvironmentAwareThirdPartyApplicationConnector,
    thirdPartyDeveloperConnector: ThirdPartyDeveloperConnector,
    clock: Clock
  )(implicit ec: ExecutionContext
  ) {

  def handleRequestCommand(app: Application, cmd: AddCollaboratorRequest)(implicit hc: HeaderCarrier): Future[AddCollaborator] = {

    for {
      admins        <- thirdPartyDeveloperConnector.fetchByEmails(getApplicationAdmins(app))
      verifiedAdmins = admins.filter(_.verified).map(_.email).toSet
      userId        <- getUserId(cmd.collaboratorEmail)
      collaborator   = Collaborator(cmd.collaboratorEmail, cmd.collaboratorRole, userId)
    } yield AddCollaborator(cmd.actor, collaborator, verifiedAdmins, LocalDateTime.now(clock))
  }

  def handleRequestCommand(app: Application, cmd: RemoveCollaboratorRequest)(implicit hc: HeaderCarrier): Future[RemoveCollaborator] = {
    for {
      admins        <- thirdPartyDeveloperConnector.fetchByEmails(getApplicationAdmins(app))
      verifiedAdmins = admins.filter(_.verified).map(_.email).toSet
      userId        <- getUserId(cmd.collaboratorEmail)
      collaborator   = Collaborator(cmd.collaboratorEmail, cmd.collaboratorRole, userId)
    } yield RemoveCollaborator(cmd.actor, collaborator, verifiedAdmins, LocalDateTime.now(clock))
  }

  def generateCreateRequest(app: Application, email: LaxEmailAddress, role: Collaborators.Role, requestingEmail: Option[LaxEmailAddress])(implicit hc: HeaderCarrier): Future[AddCollaboratorToTpaRequest] = {

    for {
      otherAdmins  <- thirdPartyDeveloperConnector.fetchByEmails(getOtherAdmins(app, requestingEmail))
      adminsToEmail = otherAdmins.filter(_.verified).map(_.email)
      userId       <- getUserId(email)
      collaborator  = Collaborator(email, role, userId)
      // TODO: handle requestingEmail being None when called from GK
      // TODO: AddCollaboratorToTpaRequest.isRegistered flag is being hard coded here as it isn't used in TPA
      request       = AddCollaboratorToTpaRequest(requestingEmail.getOrElse(LaxEmailAddress("")), collaborator, isRegistered = true, adminsToEmail.toSet)
    } yield request
  }

  @deprecated("remove after clients are no longer using the old endpoint")
  def addCollaborator(app: Application, email: LaxEmailAddress, role: Collaborators.Role, requestingEmail: Option[LaxEmailAddress])(implicit hc: HeaderCarrier): Future[AddCollaboratorResult] = {
    for {
      request  <- generateCreateRequest(app: Application, email: LaxEmailAddress, role: Collaborators.Role, requestingEmail: Option[LaxEmailAddress])
      response <- thirdPartyApplicationConnector(app.deployedTo).addCollaborator(app.id, request)
    } yield response
  }

  private def getOtherAdmins(app: Application, requestingEmail: Option[LaxEmailAddress]): Set[LaxEmailAddress] = {
    getApplicationAdmins(app).filterNot(requestingEmail.contains(_))
  }

  private def getApplicationAdmins(app: Application): Set[LaxEmailAddress] = {
    app.collaborators
      .filter(_.isAdministrator)
      .map(_.emailAddress)
  }

  private def getUserId(collaboratorEmail: LaxEmailAddress)(implicit hc: HeaderCarrier): Future[UserId] =
    thirdPartyDeveloperConnector
      .getOrCreateUserId(GetOrCreateUserIdRequest(collaboratorEmail)).map(getOrCreateUserIdResponse => getOrCreateUserIdResponse.userId)

}
