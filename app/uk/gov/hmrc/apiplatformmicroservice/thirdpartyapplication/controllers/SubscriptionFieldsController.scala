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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.controllers

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

import play.api.libs.json._
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApiContext, ApiIdentifier, ApiVersionNbr, ClientId, Environment}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.EnvironmentAwareSubscriptionFieldsConnector
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.SubscriptionFieldsConnectorDomain.JsonFormatters._

@Singleton
class SubscriptionFieldsController @Inject() (
    cc: ControllerComponents,
    subscriptionsFieldsConnector: EnvironmentAwareSubscriptionFieldsConnector
  )(implicit ec: ExecutionContext
  ) extends BackendController(cc) {

  def upsertSubscriptionFields(environment: Environment, clientId: ClientId, apiContext: ApiContext, apiVersionNbr: ApiVersionNbr): Action[JsValue] =
    Action.async(parse.json) { implicit request =>
      subscriptionsFieldsConnector(environment).upsertFieldValues(clientId, ApiIdentifier(apiContext, apiVersionNbr), request.body).map(response =>
        Status(response.status)(Json.toJson(response.body))
      )
    }
}
