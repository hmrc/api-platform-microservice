/*
 * Copyright 2025 HM Revenue & Customs
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

import org.apache.pekko.util.ByteString

import play.api.http.ContentTypes
import play.api.http.HttpEntity.Strict
import play.api.mvc.{Action, AnyContent, ControllerComponents, ResponseHeader, Result}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.common.ApplicationLogger
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.QueryConnector

@Singleton()
class QueryController @Inject() (
    queryConnector: QueryConnector,
    cc: ControllerComponents
  )(implicit val ec: ExecutionContext
  ) extends BackendController(cc) with ApplicationLogger {

  def queryEnv(environment: Environment): Action[AnyContent] = Action.async { implicit request =>
    queryConnector.query(environment, request.queryString).map(convertToResult)
  }

  private def convertToResult(resp: HttpResponse) = {
    Result(ResponseHeader(resp.status), Strict(ByteString(resp.body), Some(ContentTypes.JSON)))
      .withHeaders(resp.headers.toSeq.map(a => (a._1, a._2.head)): _*)
  }
}
