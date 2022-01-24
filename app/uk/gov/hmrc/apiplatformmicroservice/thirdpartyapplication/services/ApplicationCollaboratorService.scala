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
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.{Application, Collaborator, Role}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApplicationCollaboratorService @Inject() (
    thirdPartyApplicationConnector: EnvironmentAwareThirdPartyApplicationConnector,
    thirdPartyDeveloperConnector: ThirdPartyDeveloperConnector
  )(implicit ec: ExecutionContext)
    extends Recoveries {

      def addCollaborator(app: Application, email: String, role: Role, requestingEmail: Option[String])
                         (implicit hc: HeaderCarrier): Future[AddCollaboratorResult] = {

        def getUserId(collaboratorEmail: String): Future[UserId] =
          thirdPartyDeveloperConnector.getOrCreateUserId(GetOrCreateUserIdRequest(collaboratorEmail)).map(getOrCreateUserIdResponse => getOrCreateUserIdResponse.userId)    

        val otherAdminEmails = app.collaborators
          .filter(_.role.isAdministrator)
          .map(_.emailAddress)
          .filterNot(requestingEmail.contains(_))

        for {
          otherAdmins <- thirdPartyDeveloperConnector.fetchByEmails(otherAdminEmails)
          adminsToEmail = otherAdmins.filter(_.verified).map(_.email)
          userId <- getUserId(email)
          collaborator = Collaborator(email, role, Some(userId))
          //TODO: handle requestingEmail being None when called from GK
          //TODO: AddCollaboratorToTpaRequest.isRegistered flag is being hard coded here as it isn't used in TPA
          request = AddCollaboratorToTpaRequest(requestingEmail.getOrElse(""), collaborator, true, adminsToEmail.toSet)
          response <- thirdPartyApplicationConnector(app.deployedTo).addCollaborator(app.id, request)
        } yield response
      }
    }
