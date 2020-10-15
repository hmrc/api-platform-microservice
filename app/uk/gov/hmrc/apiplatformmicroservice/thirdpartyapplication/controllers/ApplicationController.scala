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

import javax.inject.{Inject, Singleton}
import play.api.libs.json._
import play.api.mvc._
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.services.ApplicationJsonFormatters._
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.ApplicationByIdFetcher
import uk.gov.hmrc.play.bootstrap.controller.BackendController
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.SubscriptionService

import scala.concurrent.ExecutionContext
import scala.concurrent.Future.successful
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiIdentifier
import uk.gov.hmrc.apiplatformmicroservice.common.controllers.ActionBuilders
import uk.gov.hmrc.apiplatformmicroservice.common.controllers.domain.ApplicationWithSubscriptionDataRequest

@Singleton
class ApplicationController @Inject() (
    subscriptionService: SubscriptionService,
    val applicationService: ApplicationByIdFetcher,
    cc: ControllerComponents,
    applicationByIdFetcher: ApplicationByIdFetcher
  )(implicit ec: ExecutionContext)
    extends BackendController(cc) with ActionBuilders {

  def fetchAppplicationById(id: String): Action[AnyContent] = Action.async { implicit request =>
    for {
      oApp <- applicationByIdFetcher.fetchApplicationWithSubscriptionData(ApplicationId(id))
    } yield oApp.fold[Result](NotFound)(a => Ok(Json.toJson(a)))
  }

  def createSubscriptionForApplication(applicationId: ApplicationId) =
    ApplicationWithSubscriptionDataAction(applicationId).async(parse.json) { implicit request: ApplicationWithSubscriptionDataRequest[JsValue] =>

      implicit val httpRequest : Request[JsValue] = request.request

    // requiresAuthenticationForPrivilegedOrRopcApplications(applicationId).async(parse.json) { implicit request =>
      withJsonBody[ApiIdentifier] { api =>
          subscriptionService
            .createSubscriptionForApplication(request.application, request.subscriptions, api)
            .map(res => Ok(Json.obj("text" -> res)))
      }
  }
}
