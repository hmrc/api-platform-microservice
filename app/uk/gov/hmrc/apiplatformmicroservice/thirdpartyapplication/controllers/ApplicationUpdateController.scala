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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.controllers

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import uk.gov.hmrc.apiplatform.modules.applications.domain.models.ApplicationId
import uk.gov.hmrc.apiplatformmicroservice.common.ApplicationLogger
import uk.gov.hmrc.apiplatformmicroservice.common.connectors.AuthConnector
import uk.gov.hmrc.apiplatformmicroservice.common.controllers.ActionBuilders

import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.services.ApplicationJsonFormatters
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.{ApplicationByIdFetcher, ApplicationCommandService}
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.services.ApplicationCommandJsonFormatters
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.Application
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.ApplicationCommand

@Singleton
class ApplicationUpdateController @Inject() (
    val authConfig: AuthConnector.Config,
    val authConnector: AuthConnector,
    val applicationService: ApplicationByIdFetcher,
    val applicationCommandService: ApplicationCommandService,
    cc: ControllerComponents
  )(implicit val ec: ExecutionContext
  ) extends BackendController(cc) with ActionBuilders with ApplicationLogger with ApplicationCommandJsonFormatters with ApplicationJsonFormatters {

  def update(id: ApplicationId): Action[JsValue] = Action.async(parse.json) { implicit request =>
    def handleUpdate(app: Application, command: ApplicationCommand): Future[Result] = {
      applicationCommandService.sendCommand(app, command)
        .map(app => Ok(Json.toJson(app)))
    }

    withJsonBody[ApplicationCommand] { command =>
      for {
        mayBeApplication <- applicationService.fetchApplication(id)
        responseStatus   <- mayBeApplication.fold(Future.successful(NotFound("")))(app => handleUpdate(app, command))
      } yield responseStatus
    }
  }
}
