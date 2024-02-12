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
import scala.concurrent.ExecutionContext

import cats.data.OptionT
import org.apache.pekko.stream.Materializer

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ServiceName
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ResourceId
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.services._
import uk.gov.hmrc.apiplatformmicroservice.common.StreamedResponseResourceHelper
import uk.gov.hmrc.apiplatformmicroservice.common.controllers._

@Singleton()
class ExtendedApiDefinitionController @Inject() (
    cc: ControllerComponents,
    apiDefinitionsForCollaboratorFetcher: ApiDefinitionsForCollaboratorFetcher,
    extendedApiDefinitionForCollaboratorFetcher: ExtendedApiDefinitionForCollaboratorFetcher,
    apiDocumentationResourceFetcher: ApiDocumentationResourceFetcher,
    subscribedApiDefinitionsForCollaboratorFetcher: SubscribedApiDefinitionsForCollaboratorFetcher
  )(implicit override val ec: ExecutionContext,
    override val mat: Materializer
  ) extends BackendController(cc)
    with StreamedResponseResourceHelper {

  def fetchApiDefinitionsForCollaborator(developerId: Option[UserId]): Action[AnyContent] = Action.async { implicit request =>
    apiDefinitionsForCollaboratorFetcher.fetch(developerId) map { definitions =>
      Ok(Json.toJson(definitions))
    } recover recovery
  }

  def fetchSubscribedApiDefinitionsForCollaborator(developerId: UserId): Action[AnyContent] = Action.async { implicit request =>
    subscribedApiDefinitionsForCollaboratorFetcher.fetch(developerId) map { definitions =>
      Ok(Json.toJson(definitions))
    } recover recovery
  }

  def fetchExtendedApiDefinitionForCollaborator(serviceName: ServiceName, developerId: Option[UserId]): Action[AnyContent] = Action.async { implicit request =>
    extendedApiDefinitionForCollaboratorFetcher.fetch(serviceName, developerId) map {
      case Some(extendedDefinition) => Ok(Json.toJson(extendedDefinition))
      case _                        => NotFound
    } recover recovery
  }

  def fetchApiDocumentationResource(serviceName: ServiceName, versionNbr: ApiVersionNbr, resource: String): Action[AnyContent] = Action.async { implicit request =>
    import cats.implicits._

    val resourceId = ResourceId(serviceName, versionNbr, resource)
    OptionT(apiDocumentationResourceFetcher.fetch(resourceId))
      .getOrElseF(failedDueToNotFoundException(resourceId))
      .map(handler(resourceId))
  }
}
