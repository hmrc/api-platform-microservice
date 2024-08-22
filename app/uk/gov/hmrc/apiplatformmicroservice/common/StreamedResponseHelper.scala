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

import scala.concurrent.ExecutionContext

import org.apache.pekko.stream.Materializer

import play.api.http.{HeaderNames, HttpEntity, MimeTypes, Status}
import play.api.mvc.Result
import play.api.mvc.Results._
import uk.gov.hmrc.http.HttpResponse

import uk.gov.hmrc.apiplatformmicroservice.common.controllers.{ErrorCode, JsErrorResponse}

object StreamedResponseHelper {
  val PROXY_SAFE_CONTENT_TYPE = "Proxy-Safe-Content-Type"
}

trait StreamedResponseHelper extends ApplicationLogger {
  implicit val mat: Materializer
  implicit val ec: ExecutionContext

  import StreamedResponseHelper._

  type StreamedResponseHandlerPF = PartialFunction[HttpResponse, Result]

  def getContentType(response: HttpResponse): String = {
    response.header(PROXY_SAFE_CONTENT_TYPE).orElse(response.header(HeaderNames.CONTENT_TYPE)).getOrElse(MimeTypes.TEXT)
  }

  def handleOkStreamedResponse: StreamedResponseHandlerPF = {
    case response: HttpResponse if response.status == Status.OK =>
      // Get the content type
      val contentType = getContentType(response)

      // If there's a content length, send that, otherwise return the body chunked
      response.headers.get("Content-Length") match {
        case Some(Seq(length)) =>
          Ok.sendEntity(
            HttpEntity.Streamed(response.bodyAsSource, Some(length.toLong), Some(contentType))
          )
        case _                 =>
          Ok.sendEntity(
            HttpEntity.Streamed(response.bodyAsSource, None, Some(contentType))
          )
      }
  }

  def handleErrorsAsInternalServerError(
      msg: String
    ): StreamedResponseHandlerPF = {
    case response: HttpResponse =>
      logger.warn(s"Failed due to $msg with status ${response.status}")
      InternalServerError(JsErrorResponse(ErrorCode.UNKNOWN_ERROR, "An unexpected error occurred"))
  }

  def streamedResponseAsResult(
      handleError: StreamedResponseHandlerPF
    )(
      streamedResponse: HttpResponse
    ): Result = {
    logger.info(s"Streamed Response status ${streamedResponse.status}")
    val fn = handleOkStreamedResponse orElse handleError

    if (fn.isDefinedAt(streamedResponse)) {
      fn(streamedResponse)
    } else {
      BadGateway
    }
  }
}
