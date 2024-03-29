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

package uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.controllers

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

import org.apache.pekko.stream.Materializer

import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import uk.gov.hmrc.apiplatformmicroservice.common.StreamedResponseResourceHelper
import uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.domain.services.PushPullNotificationJsonFormatters._
import uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.services.BoxFetcher

@Singleton()
class PushPullNotificationsController @Inject() (boxFetcher: BoxFetcher, cc: ControllerComponents)(implicit override val ec: ExecutionContext, override val mat: Materializer)
    extends BackendController(cc) with StreamedResponseResourceHelper {

  def getAll(): Action[AnyContent] = Action.async { implicit request =>
    boxFetcher.fetchAllBoxes().map(boxes => {
      Ok(Json.toJson(boxes))
    })
  }
}
