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

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.apiplatformmicroservice.common.Recoveries
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.UserId
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.domain.{AddCollaboratorToTpaRequest, GetOrCreateUserIdRequest}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.{AddCollaboratorResult, EnvironmentAwareThirdPartyApplicationConnector, ThirdPartyDeveloperConnector}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.{AddCollaborator, AddCollaboratorGatekeeper, AddCollaboratorGatekeeperRequest, AddCollaboratorRequest, Application, Collaborator, Role}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApplicationCollaboratorService @Inject() (
    thirdPartyApplicationConnector: EnvironmentAwareThirdPartyApplicationConnector,
    thirdPartyDeveloperConnector: ThirdPartyDeveloperConnector
  )(implicit ec: ExecutionContext) {

  def handleRequestCommand(app: Application, cmd: AddCollaboratorRequest)(implicit hc: HeaderCarrier): Future[AddCollaborator] ={
    for {
      otherAdmins <- thirdPartyDeveloperConnector.fetchByEmails(getOtherAdmins(app, Option(cmd.email)))
      adminsToEmail = otherAdmins.filter(_.verified).map(_.email).toSet
      userId <- getUserId(cmd.collaboratorEmail)
      collaborator = Collaborator(cmd.collaboratorEmail, cmd.collaborator, Some(userId))
    } yield AddCollaborator(cmd.instigator, cmd.email, collaborator, adminsToEmail, LocalDateTime.now)
  }

  def handleRequestCommand(app: Application, cmd: AddCollaboratorGatekeeperRequest)(implicit hc: HeaderCarrier): Future[AddCollaboratorGatekeeper] = {
    for {
      otherAdmins <- thirdPartyDeveloperConnector.fetchByEmails(getOtherAdmins(app, None))
      adminsToEmail = otherAdmins.filter(_.verified).map(_.email).toSet
      userId <- getUserId(cmd.collaboratorEmail)
      collaborator = Collaborator(cmd.collaboratorEmail, cmd.collaborator, Some(userId))
    } yield AddCollaboratorGatekeeper(cmd.gatekeeperUser, collaborator, adminsToEmail, LocalDateTime.now)
  }




  def generateCreateRequest(app: Application, email: String, role: Role, requestingEmail: Option[String])(implicit hc: HeaderCarrier):
  Future[AddCollaboratorToTpaRequest] = {

    for {
      otherAdmins <- thirdPartyDeveloperConnector.fetchByEmails(getOtherAdmins(app, requestingEmail))
      adminsToEmail = otherAdmins.filter(_.verified).map(_.email)
      userId <- getUserId(email)
      collaborator = Collaborator(email, role, Some(userId))
      //TODO: handle requestingEmail being None when called from GK
      //TODO: AddCollaboratorToTpaRequest.isRegistered flag is being hard coded here as it isn't used in TPA
      request = AddCollaboratorToTpaRequest(requestingEmail.getOrElse(""), collaborator, isRegistered = true, adminsToEmail.toSet)
    } yield request
  }
      @deprecated("remove after clients are no longer using the old endpoint")
      def addCollaborator(app: Application, email: String, role: Role, requestingEmail: Option[String])
                         (implicit hc: HeaderCarrier): Future[AddCollaboratorResult] = {
        for{
          request <- generateCreateRequest(app: Application, email: String, role: Role, requestingEmail: Option[String])
          response <- thirdPartyApplicationConnector(app.deployedTo).addCollaborator(app.id, request)
        } yield response
      }

  private def getOtherAdmins(app: Application, requestingEmail: Option[String]) ={
     app.collaborators
      .filter(_.role.isAdministrator)
      .map(_.emailAddress)
      .filterNot(requestingEmail.contains(_))
  }
  private def getUserId(collaboratorEmail: String)(implicit hc: HeaderCarrier): Future[UserId] =
    thirdPartyDeveloperConnector
      .getOrCreateUserId(GetOrCreateUserIdRequest(collaboratorEmail)).map(getOrCreateUserIdResponse => getOrCreateUserIdResponse.userId)

}
