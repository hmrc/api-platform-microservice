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

import uk.gov.hmrc.apiplatformmicroservice.common.ApplicationLogger
import uk.gov.hmrc.apiplatformmicroservice.common.connectors.AuthConnector
import uk.gov.hmrc.apiplatformmicroservice.common.controllers.ActionBuilders
import uk.gov.hmrc.apiplatformmicroservice.common.controllers.domain.{ApplicationRequest, ApplicationWithSubscriptionDataRequest}
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.ApplicationId
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.{AddCollaboratorSuccessResult, CollaboratorAlreadyExistsFailureResult}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.controllers.domain.{AddCollaboratorRequestOld, UpliftRequest}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.services.ApplicationJsonFormatters._
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.{
  ApplicationByIdFetcher,
  ApplicationCollaboratorService,
  SubordinateApplicationFetcher,
  UpliftApplicationService
}
import uk.gov.hmrc.apiplatform.modules.apis.domain.models._

object ApplicationController {
  import play.api.libs.functional.syntax._
  import cats.implicits._

  case class RequestUpliftV1(subscriptions: Set[ApiIdentifier])
  implicit val readsV1: Reads[RequestUpliftV1] = Json.reads[RequestUpliftV1]

  case class RequestUpliftV2(upliftRequest: UpliftRequest)
  implicit val readsV2: Reads[RequestUpliftV2] = Json.reads[RequestUpliftV2]

  implicit val reads: Reads[Either[RequestUpliftV1, RequestUpliftV2]] = readsV2.map((_).asRight[RequestUpliftV1]) or readsV1.map((_).asLeft[RequestUpliftV2])
}

@Singleton
class ApplicationController @Inject() (
    val applicationService: ApplicationByIdFetcher,
    val authConfig: AuthConnector.Config,
    val authConnector: AuthConnector,
    val applicationCollaboratorService: ApplicationCollaboratorService,
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

  @deprecated("remove after clients are no longer using the old endpoint")
  def addCollaborator(applicationId: ApplicationId): Action[JsValue] =
    ApplicationAction(applicationId).async(parse.json) { implicit request: ApplicationRequest[JsValue] =>
      withJsonBody[AddCollaboratorRequestOld] { collaboratorRequest =>
        applicationCollaboratorService.addCollaborator(request.application, collaboratorRequest.email, collaboratorRequest.role, collaboratorRequest.requestingEmail)
          .map {
            case AddCollaboratorSuccessResult(_)        => Created
            case CollaboratorAlreadyExistsFailureResult => Conflict(Json.toJson(Map("message" -> "Collaborator already exists on the Appication")))
          }
      }
    }

  import ApplicationController._

  def upliftApplication(sandboxId: ApplicationId): Action[JsValue] =
    ApplicationWithSubscriptionDataAction(sandboxId).async(parse.json) { implicit appData: ApplicationWithSubscriptionDataRequest[JsValue] =>
      withJsonBody[Either[RequestUpliftV1, RequestUpliftV2]] { upliftRequest =>
        logger.info(s"Uplift of application id ${sandboxId.value} called ${appData.application.name}")

        upliftRequest
          .fold(
            v1 => upliftApplicationService.upliftApplicationV1(appData.application, appData.subscriptions, v1.subscriptions),
            v2 => upliftApplicationService.upliftApplicationV2(appData.application, appData.subscriptions, v2.upliftRequest)
          )
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
