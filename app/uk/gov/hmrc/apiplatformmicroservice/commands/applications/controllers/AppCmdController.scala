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

package uk.gov.hmrc.apiplatformmicroservice.commands.applications.controllers

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

import cats.data.NonEmptyList
import cats.implicits.catsStdInstancesForFuture

import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.common.domain.services.NonEmptyListFormatters._
import uk.gov.hmrc.apiplatform.modules.common.services.EitherTHelper
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.commands.applications.connectors.AppCmdConnector
import uk.gov.hmrc.apiplatformmicroservice.commands.applications.services.AppCmdPreprocessor
import uk.gov.hmrc.apiplatformmicroservice.common.ApplicationLogger
import uk.gov.hmrc.apiplatformmicroservice.common.connectors.AuthConnector
import uk.gov.hmrc.apiplatformmicroservice.common.controllers.ActionBuilders
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.ApplicationByIdFetcher

@Singleton
class AppCmdController @Inject() (
    val applicationService: ApplicationByIdFetcher,
    val authConfig: AuthConnector.Config,
    val authConnector: AuthConnector,
    preprocessor: AppCmdPreprocessor,
    cmdConnector: AppCmdConnector,
    cc: ControllerComponents
  )(implicit val ec: ExecutionContext
  ) extends BackendController(cc)
    with ActionBuilders
    with ApplicationLogger {

  val E = EitherTHelper.make[NonEmptyList[CommandFailure]]

  def dispatch(id: ApplicationId): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[DispatchRequest] { inboundDispatchRequest =>
      (for {
        application             <- E.fromOptionF(applicationService.fetchApplication(id), NonEmptyList.one(CommandFailures.ApplicationNotFound)) // TODO - do we need this or should each preprocess fetch what it needs
        outboundDispatchRequest <- preprocessor.process(application, inboundDispatchRequest)
        responseStatus          <- E.fromEitherF(cmdConnector.dispatch(application.id, outboundDispatchRequest))
      } yield responseStatus)
        .fold(
          failures => BadRequest(Json.toJson(failures)),
          success => Ok(Json.toJson(success))
        )
    }
  }
}
