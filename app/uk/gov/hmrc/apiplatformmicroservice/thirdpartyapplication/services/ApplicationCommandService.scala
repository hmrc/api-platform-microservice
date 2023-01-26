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

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.{AddCollaboratorRequest, ApplicationCommand, RemoveCollaboratorRequest}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.EnvironmentAwareThirdPartyApplicationConnector
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.Application

@Singleton
class ApplicationCommandService @Inject() (
    val collaboratorService: ApplicationCollaboratorService,
    val thirdPartyApplicationConnector: EnvironmentAwareThirdPartyApplicationConnector
  ) {

  def sendCommand(app: Application, command: ApplicationCommand)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Application] = {

    def callTpa(command: ApplicationCommand): Future[Application] = {
      thirdPartyApplicationConnector(app.deployedTo).sendCommand(app.id, command)
    }

    for {
      updateCommand <- handleRequestTypes(app, command)
      result        <- callTpa(updateCommand)
    } yield result

  }

  private def handleRequestTypes(app: Application, command: ApplicationCommand)(implicit hc: HeaderCarrier): Future[ApplicationCommand] = {

    command match {
      case x: AddCollaboratorRequest    => collaboratorService.handleAddCollaboratorRequest(app, x)
      case x: RemoveCollaboratorRequest => collaboratorService.handleRemoveCollaboratorRequest(app, x)
      case x                            => Future.successful(x)
    }
  }

}
