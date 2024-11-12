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

package uk.gov.hmrc.apiplatformmicroservice.apidefinition.controllers

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import uk.gov.hmrc.apiplatform.modules.common.domain.models.Environment.{PRODUCTION, SANDBOX}
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.services._
import uk.gov.hmrc.apiplatformmicroservice.common.connectors.AuthConnector
import uk.gov.hmrc.apiplatformmicroservice.common.controllers.ActionBuilders
import uk.gov.hmrc.apiplatformmicroservice.common.controllers.domain.{ApplicationRequest, ApplicationWithSubscriptionDataRequest}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.ApplicationByIdFetcher

@Singleton
class ApiDefinitionController @Inject() (
    val applicationService: ApplicationByIdFetcher,
    applicationBasedApiFetcher: ApiDefinitionsForApplicationFetcher,
    apiDefinitionService: EnvironmentAwareApiDefinitionService,
    openAccessApisFetcher: OpenAccessApisFetcher,
    apisFetcher: ApisFetcher,
    val authConfig: AuthConnector.Config,
    val authConnector: AuthConnector,
    controllerComponents: ControllerComponents,
    apiIdentifiersForUpliftFetcher: ApiIdentifiersForUpliftFetcher,
    apiEventsFetcher: ApiEventsFetcher
  )(implicit val ec: ExecutionContext
  ) extends BackendController(controllerComponents)
    with ActionBuilders {

  private def toJson(fetch: => Future[List[ApiDefinition]]): Future[Result] = {
    for {
      defs <- fetch
    } yield Ok(Json.toJson(defs))
  }

  def fetchAllOpenApis(environment: Environment): Action[AnyContent] = Action.async { implicit request =>
    toJson(openAccessApisFetcher.fetchAllForEnvironment(environment))
  }

  def fetchAllSubscribeableApis(applicationId: ApplicationId, restricted: Option[Boolean] = Some(true)): Action[AnyContent] =
    if (restricted.getOrElse(true)) {
      applicationWithSubscriptionDataAction(applicationId).async { implicit request: ApplicationWithSubscriptionDataRequest[_] =>
        toJson(applicationBasedApiFetcher.fetchRestricted(request.deployedTo, request.subscriptions))
      }
    } else {
      applicationAction(applicationId).async { implicit request: ApplicationRequest[_] =>
        toJson(applicationBasedApiFetcher.fetchUnrestricted(request.deployedTo))
      }
    }

  def fetchAllApis(environment: Environment): Action[AnyContent] = Action.async { implicit request =>
    toJson(apisFetcher.fetchAllForEnvironment(environment))
  }

  def fetchAllUpliftableApiIdentifiers(): Action[AnyContent] = Action.async { implicit request =>
    apiIdentifiersForUpliftFetcher.fetch.map(xs => Ok(Json.toJson(xs)))
  }

  def fetchApiForServiceName(serviceName: ServiceName): Action[AnyContent] = Action.async { implicit request =>
    implicit val formatter: OFormat[Locator[ApiDefinition]] = Locator.buildLocatorFormatter[ApiDefinition]
    val sandboxFuture                                       = apiDefinitionService(Environment.SANDBOX).fetchDefinition(serviceName)
    val productionFuture                                    = apiDefinitionService(Environment.PRODUCTION).fetchDefinition(serviceName)

    for {
      maybeSandbox    <- sandboxFuture
      maybeProduction <- productionFuture
    } yield (maybeSandbox, maybeProduction) match {
      case (Some(sand), Some(prod)) => Ok(Json.toJson[Locator[ApiDefinition]](Locator.Both(sand, prod)))
      case (Some(sand), None)       => Ok(Json.toJson[Locator[ApiDefinition]](Locator.Sandbox(sand)))
      case (None, Some(prod))       => Ok(Json.toJson[Locator[ApiDefinition]](Locator.Production(prod)))
      case (None, None)             => NotFound
    }
  }

  def fetchAllNonOpenApis(environment: Environment): Action[AnyContent] = Action.async { implicit request =>
    for {
      apis <- apiDefinitionService(environment).fetchAllNonOpenAccessApiDefinitions
    } yield Ok(Json.toJson(apis))
  }

  def fetchApiEventsForServiceName(serviceName: ServiceName, includeNoChange: Boolean = true): Action[AnyContent] = Action.async { implicit request =>
    for {
      sandboxApiEvents <- apiEventsFetcher.fetchApiVersionsForEnvironment(SANDBOX, serviceName, includeNoChange)
      prodApiEvents    <- apiEventsFetcher.fetchApiVersionsForEnvironment(PRODUCTION, serviceName, includeNoChange)
    } yield Ok(Json.toJson(sandboxApiEvents ++ prodApiEvents))
  }
}
