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

package uk.gov.hmrc.apiplatformmicroservice.common

import scala.util.control.NonFatal

import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json.{Format, JsObject, Json}
import play.api.mvc.Result
import play.api.mvc.Results.{InternalServerError, NotFound}
import uk.gov.hmrc.http.NotFoundException

import uk.gov.hmrc.apiplatform.modules.common.domain.services.EnumJsonHelper

package object controllers extends ApplicationLogger {

  enum ErrorCode {
    case UnknownError, SubscriptionAlreadyExists, ApplicationNotFound, SubscriptionDenied, InvalidRequestPayload
  }

  object JsErrorResponse {

    def apply(errorCode: ErrorCode, message: JsValueWrapper): JsObject =
      Json.obj(
        "code"    -> EnumJsonHelper.toScreamingSnakeCase(errorCode),
        "message" -> message
      )
  }

  def recovery: PartialFunction[Throwable, Result] = {
    case _: NotFoundException => NotFound
    case NonFatal(e)          =>
      logger.error(s"Error occurred: ${e.getMessage}", e)
      handleException(e)
  }

  def handleException(e: Throwable) = {
    logger.error(s"An unexpected error occurred: ${e.getMessage}", e)
    InternalServerError(JsErrorResponse(ErrorCode.UnknownError, "An unexpected error occurred"))
  }

}
