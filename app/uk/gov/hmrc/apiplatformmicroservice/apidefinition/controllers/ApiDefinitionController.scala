/*
 * Copyright 2021 HM Revenue & Customs
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
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.apiplatformmicroservice.common.controllers.ActionBuilders
import uk.gov.hmrc.apiplatformmicroservice.common.controllers.domain.ApplicationWithSubscriptionDataRequest
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.ApplicationByIdFetcher
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.collection.immutable.ListMap
import scala.concurrent.ExecutionContext
import uk.gov.hmrc.apiplatformmicroservice.common.controllers.domain.ApplicationRequest
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.services.ApiDefinitionsForApplicationFetcher
import uk.gov.hmrc.apiplatformmicroservice.common.connectors.AuthConnector
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.services.OpenAccessApisFetcher
import scala.concurrent.Future
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiDefinition
import play.api.mvc.Result
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.Environment
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.services.ApisFetcher


@Singleton
class ApiDefinitionController @Inject() (
    val applicationService: ApplicationByIdFetcher,
    applicationBasedApiFetcher: ApiDefinitionsForApplicationFetcher,
    openAccessApisFetcher: OpenAccessApisFetcher,
    apisFetcher: ApisFetcher,
    val authConfig: AuthConnector.Config,
    val authConnector: AuthConnector,
    controllerComponents: ControllerComponents
  )(implicit val ec: ExecutionContext)
    extends BackendController(controllerComponents)
    with ActionBuilders
 {

  import ApiDefinitionController.JsonFormatters._
  import ApiDefinitionController._

  private def fetchApiDefinitions( fetch: => Future[List[ApiDefinition]]): Future[Result] = {
    for {
      defs <- fetch
      converted = convert(defs)
    } yield Ok(Json.toJson(converted))
  }

  def fetchAllOpenApis(environment: Environment): Action[AnyContent] = Action.async { implicit request =>
      fetchApiDefinitions( openAccessApisFetcher.fetchAllForEnvironment(environment) ) 
    }

  def fetchAllSubscribeableApis(applicationId: ApplicationId, restricted: Option[Boolean] = Some(true)): Action[AnyContent] =
    if(restricted.getOrElse(true)) {
      ApplicationWithSubscriptionDataAction(applicationId).async { implicit request: ApplicationWithSubscriptionDataRequest[_] =>
        fetchApiDefinitions( applicationBasedApiFetcher.fetchRestricted(request.application, request.subscriptions) )
      }
    } else {
      ApplicationAction(applicationId).async { implicit request: ApplicationRequest[_] => 
        fetchApiDefinitions( applicationBasedApiFetcher.fetchUnrestricted(request.application) )
      }
    }

    def fetchAllApis(environment: Environment): Action[AnyContent] = Action.async { implicit request =>
      fetchApiDefinitions( apisFetcher.fetchAllForEnvironment(environment) ) 
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
      versions: Map[ApiVersion, VersionData])

  object ApiData {
    implicit val ordering: Ordering[(ApiVersion, VersionData)] = new Ordering[(ApiVersion, VersionData)] {
      override def compare(x: (ApiVersion, VersionData), y: (ApiVersion, VersionData)): Int = y._1.value.compareTo(x._1.value)
    }

    def fromDefinition(in: ApiDefinition): ApiData = {
      val versionData = ListMap[ApiVersion, VersionData](in.versions.map(v => v.version -> VersionData.fromDefinition(v)).sorted:_*)
      ApiData(in.serviceName, in.name, in.isTestSupport, versionData)
    }
  }

  object JsonFormatters extends ApiDefinitionJsonFormatters {
    import play.api.libs.json._
    implicit val writesVersionData: OWrites[VersionData] = Json.writes[VersionData]
    implicit val writesApiData: OWrites[ApiData] = Json.writes[ApiData]
  }
}
