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

package uk.gov.hmrc.apiplatformmicroservice.subscriptionfields.controllers

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import play.api.libs.json.{JsValue, Json, Reads}
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import uk.gov.hmrc.apiplatform.modules.common.domain.models.{Environment, _}
import uk.gov.hmrc.apiplatform.modules.subscriptionfields.domain.models.{FieldErrorMap, FieldName, FieldValue}
import uk.gov.hmrc.apiplatformmicroservice.common.ApplicationLogger
import uk.gov.hmrc.apiplatformmicroservice.common.controllers.{ErrorCode, JsErrorResponse}
import uk.gov.hmrc.apiplatformmicroservice.subscriptionfields.connectors.EnvironmentAwareSubscriptionFieldsConnector

object SubscriptionFieldsController {
  type Fields = Map[FieldName, FieldValue]
  case class SubscriptionFieldsRequest(fields: Fields)
  implicit val reads: Reads[SubscriptionFieldsRequest] = Json.reads[SubscriptionFieldsRequest]
}

@Singleton
class SubscriptionFieldsController @Inject() (
    cc: ControllerComponents,
    subscriptionFieldsConnector: EnvironmentAwareSubscriptionFieldsConnector
  )(implicit ec: ExecutionContext
  ) extends BackendController(cc) with ApplicationLogger {

  def saveFieldValues(
      environment: Environment,
      clientId: ClientId,
      apiContext: ApiContext,
      apiVersionNbr: ApiVersionNbr
    ): Action[JsValue] = Action.async(parse.json) { implicit request =>
    import SubscriptionFieldsController._

    withJsonBody[SubscriptionFieldsRequest] { payload =>
      if (payload.fields.isEmpty) {
        Future.successful(UnprocessableEntity(JsErrorResponse(ErrorCode.INVALID_REQUEST_PAYLOAD, "At least one field must be specified")))
      } else {
        subscriptionFieldsConnector(environment).saveFieldValues(clientId, ApiIdentifier(apiContext, apiVersionNbr), payload.fields)
          .map(_ match {
            case Left(errs: FieldErrorMap) => BadRequest(Json.toJson(errs))
            case Right(subsFields)         => Ok
          })
      }
    }
  }
}
