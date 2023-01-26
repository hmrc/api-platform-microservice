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

import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ApiIdentifier
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.SubscribeToApi
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.services.ApplicationCommandJsonFormatters
import uk.gov.hmrc.apiplatformmicroservice.common.connectors.AuthConnector
import uk.gov.hmrc.apiplatformmicroservice.common.controllers.domain.ApplicationWithSubscriptionDataRequest
import uk.gov.hmrc.apiplatformmicroservice.common.controllers.{ActionBuilders, ErrorCode, JsErrorResponse}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.services.ApplicationJsonFormatters._
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.SubscriptionService.{CreateSubscriptionDenied, CreateSubscriptionDuplicate, CreateSubscriptionSuccess}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.{ApplicationByIdFetcher, SubscriptionService, UpliftApplicationService}

@Singleton
class SubscriptionController @Inject() (
    subscriptionService: SubscriptionService,
    val applicationService: ApplicationByIdFetcher,
    val authConfig: AuthConnector.Config,
    val authConnector: AuthConnector,
    cc: ControllerComponents,
    val upliftApplicationService: UpliftApplicationService
  )(implicit val ec: ExecutionContext
  ) extends BackendController(cc) with ActionBuilders with ApplicationCommandJsonFormatters {

  @deprecated("remove after clients are no longer using the old endpoint")
  def subscribeToApi(applicationId: ApplicationId, restricted: Option[Boolean]): Action[JsValue] =
    RequiresAuthenticationForPrivilegedOrRopcApplications(applicationId).async(parse.json) { implicit request: ApplicationWithSubscriptionDataRequest[JsValue] =>
      withJsonBody[ApiIdentifier] { api =>
        subscriptionService
          .createSubscriptionForApplication(request.application, request.subscriptions, api, restricted.getOrElse(true))
          .map {
            case CreateSubscriptionSuccess   => NoContent
            case CreateSubscriptionDenied    => NotFound(JsErrorResponse(ErrorCode.APPLICATION_NOT_FOUND, s"API $api is not available for application ${applicationId.value}"))
            case CreateSubscriptionDuplicate => Conflict(JsErrorResponse(
                ErrorCode.SUBSCRIPTION_ALREADY_EXISTS,
                s"Application: '${request.application.name}' is already Subscribed to API: ${api.context.value}: ${api.version.value}"
              ))
          }
      }
    }

  def subscribeToApiAppUpdate(applicationId: ApplicationId, restricted: Option[Boolean]): Action[JsValue] =
    RequiresAuthenticationForPrivilegedOrRopcApplications(applicationId).async(parse.json) { implicit request: ApplicationWithSubscriptionDataRequest[JsValue] =>
      withJsonBody[SubscribeToApi] { subscribeToApi =>
        val api = subscribeToApi.apiIdentifier
        subscriptionService
          .createSubscriptionForApplication(request.application, request.subscriptions, subscribeToApi, restricted.getOrElse(true))
          .map {
            case CreateSubscriptionSuccess   => NoContent
            case CreateSubscriptionDenied    => NotFound(JsErrorResponse(ErrorCode.APPLICATION_NOT_FOUND, s"API $api is not available for application ${applicationId.value}"))
            case CreateSubscriptionDuplicate => Conflict(JsErrorResponse(
                ErrorCode.SUBSCRIPTION_ALREADY_EXISTS,
                s"Application: '${request.application.name}' is already Subscribed to API: ${api.context.value}: ${api.version.value}"
              ))
          }
      }
    }

  def fetchUpliftableSubscriptions(applicationId: ApplicationId): Action[AnyContent] =
    ApplicationWithSubscriptionDataAction(applicationId).async { implicit appData: ApplicationWithSubscriptionDataRequest[AnyContent] =>
      upliftApplicationService.fetchUpliftableApisForApplication(appData.subscriptions)
        .map { set =>
          if (set.isEmpty) NotFound else Ok(Json.toJson(set))
        }
    }
}
