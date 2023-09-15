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
import scala.collection.immutable.ListMap
import scala.concurrent.{ExecutionContext, Future}

import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.ApplicationId
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiDefinition
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.services._
import uk.gov.hmrc.apiplatformmicroservice.common.connectors.AuthConnector
import uk.gov.hmrc.apiplatformmicroservice.common.controllers.ActionBuilders
import uk.gov.hmrc.apiplatformmicroservice.common.controllers.domain.{ApplicationRequest, ApplicationWithSubscriptionDataRequest}
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.Environment
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
    apiIdentifiersForUpliftFetcher: ApiIdentifiersForUpliftFetcher
  )(implicit val ec: ExecutionContext
  ) extends BackendController(controllerComponents)
    with ActionBuilders {

  import ApiDefinitionController.JsonFormatters._
  import ApiDefinitionController._

  private def fetchApiDefinitions(fetch: => Future[List[ApiDefinition]]): Future[Result] = {
    for {
      defs     <- fetch
      converted = convert(defs)
    } yield Ok(Json.toJson(converted))
  }

  def fetchAllOpenApis(environment: Environment): Action[AnyContent] = Action.async { implicit request =>
    fetchApiDefinitions(openAccessApisFetcher.fetchAllForEnvironment(environment))
  }

  def fetchAllSubscribeableApis(applicationId: ApplicationId, restricted: Option[Boolean] = Some(true)): Action[AnyContent] =
    if (restricted.getOrElse(true)) {
      applicationWithSubscriptionDataAction(applicationId).async { implicit request: ApplicationWithSubscriptionDataRequest[_] =>
        fetchApiDefinitions(applicationBasedApiFetcher.fetchRestricted(request.application, request.subscriptions))
      }
    } else {
      applicationAction(applicationId).async { implicit request: ApplicationRequest[_] =>
        fetchApiDefinitions(applicationBasedApiFetcher.fetchUnrestricted(request.application))
      }
    }

  def fetchAllApis(environment: Environment): Action[AnyContent] = Action.async { implicit request =>
    fetchApiDefinitions(apisFetcher.fetchAllForEnvironment(environment))
  }

  def fetchAllUpliftableApiIdentifiers(): Action[AnyContent] = Action.async { implicit request =>
    apiIdentifiersForUpliftFetcher.fetch.map(xs => Ok(Json.toJson(xs)))
  }

  def fetchAllNonOpenApis(environment: Environment): Action[AnyContent] = Action.async { implicit request =>
    for {
      apis <- apiDefinitionService(environment).fetchAllNonOpenAccessApiDefinitions
    } yield Ok(Json.toJson(apis))
  }
}

object ApiDefinitionController {

  import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models._

  def convert(in: Seq[ApiDefinition]): Map[ApiContext, ApiData] = {
    in.map(d => d.context -> ApiData.fromDefinition(d)).toMap
  }

  case class VersionData(status: ApiStatus, access: ApiAccess)

  object VersionData {
    def fromDefinition(in: ApiVersionDefinition): VersionData = VersionData(in.status, in.access)
  }

  case class ApiData(
      serviceName: String,
      name: String,
      isTestSupport: Boolean,
      versions: Map[ApiVersionNbr, VersionData],
      categories: List[ApiCategory]
    )

  object ApiData {

    implicit val ordering: Ordering[(ApiVersionNbr, VersionData)] = new Ordering[(ApiVersionNbr, VersionData)] {
      override def compare(x: (ApiVersionNbr, VersionData), y: (ApiVersionNbr, VersionData)): Int = y._1.value.compareTo(x._1.value)
    }

    def fromDefinition(in: ApiDefinition): ApiData = {
      val versionData = ListMap[ApiVersionNbr, VersionData](in.versions.map(v => v.version -> VersionData.fromDefinition(v)).sorted: _*)
      ApiData(in.serviceName, in.name, in.isTestSupport, versionData, in.categories)
    }
  }

  object JsonFormatters extends ApiDefinitionJsonFormatters {
    import play.api.libs.json._
    implicit val writesVersionData: OWrites[VersionData] = Json.writes[VersionData]
    implicit val writesApiData: OWrites[ApiData]         = Json.writes[ApiData]
  }
}
