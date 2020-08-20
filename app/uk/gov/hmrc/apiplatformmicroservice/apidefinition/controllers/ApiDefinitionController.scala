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
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiDefinitionJsonFormatters
import uk.gov.hmrc.apiplatformmicroservice.common.controllers.domain.ApplicationRequest
import uk.gov.hmrc.apiplatformmicroservice.common.controllers.ActionBuilders
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.ApplicationByIdFetcher
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.concurrent.ExecutionContext
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.services.{ApiDefinitionsForApplicationFetcher, EnvironmentAwareApiDefinitionService, FilterApis}

@Singleton
class ApiDefinitionController @Inject() (
    val applicationService: ApplicationByIdFetcher,
    apiDefinitionsForApplicationFetcher: ApiDefinitionsForApplicationFetcher,
    environmentAwareApiDefinitionService: EnvironmentAwareApiDefinitionService,
    controllerComponents: ControllerComponents
  )(implicit ec: ExecutionContext)
    extends BackendController(controllerComponents)
    with ActionBuilders
    with ApiDefinitionJsonFormatters
    with FilterApis {

  def fetchAllSubscribeableApis(applicationId: ApplicationId): Action[AnyContent] = ApplicationAction(applicationId).async { implicit request: ApplicationRequest[_] =>
    for {
      defs <- environmentAwareApiDefinitionService(request.deployedTo).fetchAllDefinitions
      filtered = filterApis(Seq(applicationId))(defs)
    } yield Ok(Json.toJson(filtered))
  }
}
