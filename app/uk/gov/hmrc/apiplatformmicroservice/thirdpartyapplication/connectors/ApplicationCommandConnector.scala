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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import uk.gov.hmrc.http.{HttpClient, _}
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.ApplicationId
import uk.gov.hmrc.apiplatformmicroservice.common.{ApplicationLogger, EnvironmentAware, ProxiedHttpClient}
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models._

trait ApplicationCommandConnector {
  def dispatch(
      applicationId: ApplicationId,
      dispatchRequest: DispatchRequest
  )(
    implicit hc: HeaderCarrier
  ): ApplicationCommandHandlerTypes.Result
}

private[thirdpartyapplication] abstract class AbstractApplicationCommandConnector
    extends ApplicationCommandConnector
    with ApplicationLogger {

  implicit def ec: ExecutionContext
  val serviceBaseUrl: String
  def http: HttpClient

  def baseApplicationUrl(applicationId: ApplicationId) = s"$serviceBaseUrl/application/${applicationId.value}"

  def dispatch(
      applicationId: ApplicationId,
      dispatchRequest: DispatchRequest
    )(implicit hc: HeaderCarrier
    ): ApplicationCommandHandlerTypes.Result = {

    import uk.gov.hmrc.apiplatform.modules.common.domain.services.NonEmptyChainFormatters._
    import play.api.libs.json._
    import uk.gov.hmrc.http.HttpReads.Implicits._
    import play.api.http.Status._

    def parseWithLogAndThrow[T](input: String)(implicit reads: Reads[T]): T = {
      Json.parse(input).validate[T] match {
        case JsSuccess(t, _) => t
        case JsError(err) =>
          logger.error(s"Failed to parse >>$input<< due to errors $err")
          throw new InternalServerException("Failed parsing response to dispatch")
      }
    }

    val url          = s"${baseApplicationUrl(applicationId)}/dispatch"
    val extraHeaders = Seq.empty[(String, String)]
    import cats.syntax.either._

    http.PATCH[DispatchRequest, HttpResponse](url, dispatchRequest, extraHeaders)
      .map(response =>
        response.status match {
          case OK          => parseWithLogAndThrow[DispatchSuccessResult](response.body).asRight[ApplicationCommandHandlerTypes.Failures]
          case BAD_REQUEST => parseWithLogAndThrow[ApplicationCommandHandlerTypes.Failures](response.body).asLeft[DispatchSuccessResult]
          case status      => logger.error(s"Dispatch failed with status code: $status")
                              throw new InternalServerException(s"Failed calling dispatch $status")
        }
      )
  }
}

@Singleton
class SubordinateApplicationCommandConnector @Inject() (
    config: SubordinateApplicationCommandConnector.Config,
    httpClient: HttpClient,
    val proxiedHttpClient: ProxiedHttpClient
  )(implicit override val ec: ExecutionContext
  ) extends AbstractApplicationCommandConnector {

    import config._
    val serviceBaseUrl: String = config.baseUrl

  lazy val http: HttpClient = if (useProxy) proxiedHttpClient.withHeaders(bearerToken, apiKey) else httpClient
}

object SubordinateApplicationCommandConnector {
  case class Config(
      baseUrl: String,
      useProxy: Boolean,
      bearerToken: String,
      apiKey: String
    )
}


@Singleton
class PrincipalApplicationCommandConnector @Inject() (
    config: PrincipalApplicationCommandConnector.Config,
    val http: HttpClient
  )(implicit val ec: ExecutionContext
  ) extends AbstractApplicationCommandConnector {

  val serviceBaseUrl: String = config.baseUrl
}

object PrincipalApplicationCommandConnector {
  case class Config(
      baseUrl: String
    )
}

@Singleton
class EnvironmentAwareApplicationCommandConnector @Inject() (
    val subordinate: SubordinateApplicationCommandConnector,
    val principal: PrincipalApplicationCommandConnector
  ) extends EnvironmentAware[ApplicationCommandConnector]

