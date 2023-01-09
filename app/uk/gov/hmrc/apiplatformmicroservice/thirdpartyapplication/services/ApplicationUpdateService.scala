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

import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.EnvironmentAwareThirdPartyApplicationConnector
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.{AddCollaboratorRequest, Application, ApplicationUpdate, RemoveCollaboratorRequest, UpdateRequest}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApplicationUpdateService @Inject()(val collaboratorService: ApplicationCollaboratorService,
                                         val thirdPartyApplicationConnector: EnvironmentAwareThirdPartyApplicationConnector) {
  def updateApplication(app: Application, applicationUpdate: ApplicationUpdate)
                       (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Application] = {

    def callTpa(applicationUpdate: ApplicationUpdate): Future[Application] ={
      thirdPartyApplicationConnector(app.deployedTo).updateApplication(app.id, applicationUpdate)
    }

    for{
      updateCommand <- handleRequestTypes(app, applicationUpdate)
      result <- callTpa(updateCommand)
    } yield result

  }


  private def handleRequestTypes(app: Application, applicationUpdate: ApplicationUpdate)(implicit hc: HeaderCarrier): Future[ApplicationUpdate] ={

    def handleRequest(updateRequest: UpdateRequest)= {
      updateRequest match {
        case x: AddCollaboratorRequest => collaboratorService.handleRequestCommand(app, x)
        case x: RemoveCollaboratorRequest => collaboratorService.handleRequestCommand(app, x)
        case x => Future.successful(x)
      }
    }

    applicationUpdate match {
      case request: UpdateRequest => handleRequest(request)
      case x => Future.successful(x)
    }
  }

}
