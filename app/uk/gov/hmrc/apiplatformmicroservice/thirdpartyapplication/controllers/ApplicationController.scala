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
import scala.concurrent.ExecutionContext

import play.api.libs.json._
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.applications.core.interface.models.UpliftRequest
import uk.gov.hmrc.apiplatformmicroservice.common.ApplicationLogger
import uk.gov.hmrc.apiplatformmicroservice.common.connectors.AuthConnector
import uk.gov.hmrc.apiplatformmicroservice.common.controllers.ActionBuilders
import uk.gov.hmrc.apiplatformmicroservice.common.controllers.domain.ApplicationWithSubscriptionDataRequest
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.{ApplicationByIdFetcher, SubordinateApplicationFetcher, UpliftApplicationService}

object ApplicationController {

  case class RequestUpliftV2(upliftRequest: UpliftRequest)
  implicit val readsV2: Reads[RequestUpliftV2] = Json.reads[RequestUpliftV2]

}

@Singleton
class ApplicationController @Inject() (
    val applicationService: ApplicationByIdFetcher,
    val authConfig: AuthConnector.Config,
    val authConnector: AuthConnector,
    val upliftApplicationService: UpliftApplicationService,
    val subordinateApplicationFetcher: SubordinateApplicationFetcher,
    cc: ControllerComponents
  )(implicit val ec: ExecutionContext
  ) extends BackendController(cc) with ActionBuilders with ApplicationLogger {

  def fetchAppplicationById(id: ApplicationId): Action[AnyContent] = Action.async { implicit request =>
    for {
      oApp <- applicationService.fetchApplicationWithSubscriptionData(id)
    } yield oApp.fold[Result](NotFound)(a => Ok(Json.toJson(a)))
  }

  import ApplicationController._

  def upliftApplication(sandboxId: ApplicationId): Action[JsValue] =
    applicationWithSubscriptionDataAction(sandboxId).async(parse.json) { implicit appData: ApplicationWithSubscriptionDataRequest[JsValue] =>
      withJsonBody[RequestUpliftV2] { upliftRequest =>
        logger.info(s"Uplift of application id ${sandboxId} called ${appData.application.name}")

        upliftApplicationService.upliftApplicationV2(appData.application, appData.subscriptions, upliftRequest.upliftRequest)
          .map(
            _.fold(
              msg => BadRequest(Json.toJson(Map("message" -> msg))),
              id => Created(Json.toJson(id))
            )
          )
      }
    }

  def fetchLinkedSubordinateApplication(principalApplicationId: ApplicationId): Action[AnyContent] = Action.async { implicit request =>
    for {
      subordinateApplication <- subordinateApplicationFetcher.fetchSubordinateApplication(principalApplicationId)
    } yield subordinateApplication.fold[Result](NotFound)(application => Ok(Json.toJson(application)))
  }
}
