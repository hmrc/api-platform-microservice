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

package uk.gov.hmrc.apiplatformmicroservice.commands.applications.connectors

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, _}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.commands.applications.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.common.ApplicationLogger

@Singleton
class AppCmdConnector @Inject() (
    config: AppCmdConnector.Config,
    val http: HttpClientV2
  )(implicit val ec: ExecutionContext
  ) extends ApplicationLogger {

  val serviceBaseUrl: String = config.baseUrl

  def baseApplicationUrl(applicationId: ApplicationId) = s"$serviceBaseUrl/applications/${applicationId}"

  def dispatch(
      applicationId: ApplicationId,
      dispatchRequest: DispatchRequest
    )(implicit hc: HeaderCarrier
    ): AppCmdHandlerTypes.AppCmdResult = {

    import uk.gov.hmrc.apiplatform.modules.common.domain.services.NonEmptyListFormatters._
    import play.api.libs.json._
    import uk.gov.hmrc.http.HttpReads.Implicits._
    import play.api.http.Status._

    def parseWithLogAndThrow[T](input: String)(implicit reads: Reads[T]): T = {
      Json.parse(input).validate[T] match {
        case JsSuccess(t, _) => t
        case JsError(err)    =>
          logger.error(s"Failed to parse >>$input<< due to errors $err")
          throw new InternalServerException("Failed parsing response to dispatch")
      }
    }

    import cats.syntax.either._

    http.patch(url"${baseApplicationUrl(applicationId)}/dispatch")
      .withBody(Json.toJson(dispatchRequest))
      .execute[HttpResponse]
      .map(response =>
        response.status match {
          case OK           => parseWithLogAndThrow[DispatchSuccessResult](response.body).asRight[AppCmdHandlerTypes.Failures]
          case BAD_REQUEST  => parseWithLogAndThrow[AppCmdHandlerTypes.Failures](response.body).asLeft[DispatchSuccessResult]
          case UNAUTHORIZED => throw new UnauthorizedException("Command unauthorised")
          case status       =>
            logger.error(s"Dispatch failed with status code: $status")
            throw new InternalServerException(s"Failed calling dispatch $status")
        }
      )
  }
}

object AppCmdConnector {

  case class Config(
      baseUrl: String
    )
}
