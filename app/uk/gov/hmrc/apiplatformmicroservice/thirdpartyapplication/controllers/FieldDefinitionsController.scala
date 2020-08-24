/*
 * Copyright 2020 HM Revenue & Customs
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

import play.api.mvc._
import play.api.libs.json._
import uk.gov.hmrc.play.bootstrap.controller.BackendController
import javax.inject.{Inject, Singleton}

import scala.concurrent.ExecutionContext
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.Environment
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.EnvironmentAwareSubscriptionFieldsConnector

@Singleton
class FieldDefinitionsController @Inject() (
    cc: ControllerComponents,
    subscriptionsFieldsConnector: EnvironmentAwareSubscriptionFieldsConnector
  )(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.services.FieldsJsonFormatters._

  def fetchFieldDefinitions(environment: Environment): Action[AnyContent] = Action.async { implicit request =>
    subscriptionsFieldsConnector(environment).bulkFetchFieldDefintions.map(fds => Ok(Json.toJson(fds)))
  }

}