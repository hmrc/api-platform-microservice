/*
 * Copyright 2021 HM Revenue & Customs
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
import play.api.libs.json._
import play.api.mvc._
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.services.ApplicationJsonFormatters._
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.controllers.domain.AddCollaboratorRequest
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.ApplicationByIdFetcher
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.ApplicationCollaboratorService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.apiplatformmicroservice.common.controllers.domain.ApplicationRequest

import scala.concurrent.ExecutionContext
import scala.concurrent.Future.successful
import uk.gov.hmrc.apiplatformmicroservice.common.controllers.ActionBuilders
import uk.gov.hmrc.apiplatformmicroservice.common.connectors.AuthConnector
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.{AddCollaboratorSuccessResult, CollaboratorAlreadyExistsFailureResult}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.UpliftApplicationService
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.services.ApplicationJsonFormatters._
import uk.gov.hmrc.apiplatformmicroservice.common.controllers.domain.ApplicationWithSubscriptionDataRequest
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiIdentifier
import uk.gov.hmrc.apiplatformmicroservice.common.ApplicationLogger

@Singleton
class ApplicationController @Inject() (
    val applicationService: ApplicationByIdFetcher,
    val authConfig: AuthConnector.Config,
    val authConnector: AuthConnector,
    val applicationCollaboratorService : ApplicationCollaboratorService,
    val upliftApplicationService: UpliftApplicationService,
    cc: ControllerComponents
  )(implicit val ec: ExecutionContext)
    extends BackendController(cc) with ActionBuilders with ApplicationLogger {

  def fetchAppplicationById(id: ApplicationId): Action[AnyContent] = Action.async { implicit request =>
    for {
      oApp <- applicationService.fetchApplicationWithSubscriptionData(id)
    } yield oApp.fold[Result](NotFound)(a => Ok(Json.toJson(a)))
  }

  def addCollaborator(applicationId: ApplicationId): Action[JsValue] =
    ApplicationAction(applicationId).async(parse.json) { implicit request: ApplicationRequest[JsValue] =>
      withJsonBody[AddCollaboratorRequest] { collaboratorRequest =>
        applicationCollaboratorService.addCollaborator(request.application, collaboratorRequest.email, collaboratorRequest.role, collaboratorRequest.requestingEmail)
          .map{
            case AddCollaboratorSuccessResult(_) => Created
            case CollaboratorAlreadyExistsFailureResult => Conflict(Json.toJson(Map("message" -> "Collaborator already exists on the Appication")))
          }
      }
    }

  def upliftApplication(sandboxId: ApplicationId): Action[JsValue] =
    ApplicationWithSubscriptionDataAction(sandboxId).async(parse.json) { implicit appData: ApplicationWithSubscriptionDataRequest[JsValue] =>
      withJsonBody[Set[ApiIdentifier]] { requestedApis => 
        logger.info(s"Uplift of application id ${sandboxId.value} called ${appData.application.name}")

        if(requestedApis.isEmpty) {
          successful(BadRequest(Json.toJson(Map("message" -> "Request contains no apis for uplifting the sandbox application"))))
        }

        else {
          upliftApplicationService.upliftApplication(appData.application, appData.subscriptions, requestedApis).map(
            _.fold(
              msg => BadRequest(Json.toJson(Map("message" -> msg))),
              id => Created(Json.toJson(id))
            )
          )
        }
      }
    }
}
