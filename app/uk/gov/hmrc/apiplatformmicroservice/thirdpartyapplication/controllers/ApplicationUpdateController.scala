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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.controllers

import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import uk.gov.hmrc.apiplatformmicroservice.common.ApplicationLogger
import uk.gov.hmrc.apiplatformmicroservice.common.connectors.AuthConnector
import uk.gov.hmrc.apiplatformmicroservice.common.controllers.ActionBuilders
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.{Application, ApplicationUpdate, ApplicationUpdateFormatters}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.services.ApplicationJsonFormatters
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.{ApplicationByIdFetcher, ApplicationUpdateService}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

object ApplicationUpdateController {
}

@Singleton
class ApplicationUpdateController @Inject()(
                                             val authConfig: AuthConnector.Config,
                                             val authConnector: AuthConnector,
                                             val applicationService: ApplicationByIdFetcher,
                                             val applicationUpdateService: ApplicationUpdateService,
                                             cc: ControllerComponents
                                           )(implicit val ec: ExecutionContext)
  extends BackendController(cc) with ActionBuilders with ApplicationLogger with ApplicationUpdateFormatters with ApplicationJsonFormatters {

  def update(id: ApplicationId): Action[JsValue] = Action.async(parse.json) { implicit request =>
    
    def handleUpdate(app: Application, applicationUpdate: ApplicationUpdate): Future[Result] = {
      applicationUpdateService.updateApplication(app, applicationUpdate)
      .map(app => Ok(Json.toJson(app)))
    }
    
    withJsonBody[ApplicationUpdate] { applicationUpdate =>
      for {
        mayBeApplication <- applicationService.fetchApplication(id)
        responseStatus <- mayBeApplication.fold(Future.successful(NotFound("")))(app => handleUpdate(app, applicationUpdate))
      } yield responseStatus
    }
  }
}
