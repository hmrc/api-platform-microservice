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

package uk.gov.hmrc.apiplatformmicroservice.apidefinition.controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.services.{ApiDefinitionsForApplicationFetcher, FilterApis}
import uk.gov.hmrc.apiplatformmicroservice.common.controllers.domain.ApplicationRequest
import uk.gov.hmrc.apiplatformmicroservice.common.controllers.ActionBuilders
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.ApplicationByIdFetcher
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.concurrent.ExecutionContext

@Singleton
class ApiDefinitionController @Inject() (
    val applicationService: ApplicationByIdFetcher,
    fetcher: ApiDefinitionsForApplicationFetcher,
    controllerComponents: ControllerComponents
  )(implicit ec: ExecutionContext)
    extends BackendController(controllerComponents)
    with ActionBuilders
    with FilterApis {

  import ApiDefinitionController._
  import ApiDefinitionController.JsonFormatters._

  def fetchAllSubscribeableApis(applicationId: ApplicationId): Action[AnyContent] =
    ApplicationAction(applicationId).async { implicit request: ApplicationRequest[_] =>
      for {
        defs <- fetcher.fetch(applicationId, request.deployedTo)
        filtered = filterApis(Seq(applicationId))(defs)
        converted = convert(filtered)
      } yield Ok(Json.toJson(converted))
    }
}

object ApiDefinitionController {

  import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models._

  def convert(in: Seq[APIDefinition]): Map[ApiContext, ApiData] = {
    in.map(d => (d.context -> ApiData.fromDefinition(d))).toMap
  }

  case class VersionData(status: APIStatus, access: APIAccess)

  object VersionData {
    def fromDefinition(in: ApiVersionDefinition): VersionData = VersionData(in.status, in.access)
  }

  case class ApiData(
      serviceName: String,
      name: String,
      isTestSupport: Boolean,
      versions: Map[ApiVersion, VersionData])

  object ApiData {

    def fromDefinition(in: APIDefinition): ApiData = {
      val versionData = in.versions.map(v => (v.version -> VersionData.fromDefinition(v))).toMap
      ApiData(in.serviceName, in.name, in.isTestSupport, versionData)
    }
  }

  object JsonFormatters extends ApiDefinitionJsonFormatters {
    import play.api.libs.json._
    implicit val writesVersionData: OWrites[VersionData] = Json.writes[VersionData]
    implicit val writesApiData: OWrites[ApiData] = Json.writes[ApiData]
  }
}
