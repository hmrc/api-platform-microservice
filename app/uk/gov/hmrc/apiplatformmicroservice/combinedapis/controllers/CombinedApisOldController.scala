/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.apiplatformmicroservice.combinedapis.controllers

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.apiplatformmicroservice.combinedapis.models.BasicCombinedApiJsonFormatters
import uk.gov.hmrc.apiplatformmicroservice.combinedapis.services.CombinedApisService
import uk.gov.hmrc.apiplatformmicroservice.common.controllers._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.UserId

@Singleton
class CombinedApisOldController @Inject()(combinedApisService: CombinedApisService, cc: ControllerComponents)
                                      (implicit ec: ExecutionContext) extends BackendController(cc) with BasicCombinedApiJsonFormatters {


  @deprecated("please use getCombinedApisForDeveloper in CombinedApisController", "2021")
  def getCombinedApisForDeveloper(userId: Option[UserId]): Action[AnyContent] = Action.async { implicit request =>
    combinedApisService.fetchCombinedApisForDeveloperId(userId)
      .map(x => Ok(Json.toJson(x))) recover recovery
  }

 @deprecated("please use fetchApiForCollaborator in CombinedApisController", "2021")
  def fetchApiForCollaborator(serviceName: String, userId: Option[UserId]) = Action.async { implicit request =>
    combinedApisService.fetchApiForCollaborator(serviceName, userId)
      .map(x => Ok(Json.toJson(x))) recover recovery

  }


}