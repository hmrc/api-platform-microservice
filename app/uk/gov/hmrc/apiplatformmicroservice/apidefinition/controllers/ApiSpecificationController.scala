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

import akka.stream.Materializer
import cats.data.OptionT
import cats.implicits._

import play.api.mvc.ControllerComponents
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ServiceName
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApiVersionNbr
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.services._
import uk.gov.hmrc.apiplatformmicroservice.common.StreamedResponseResourceHelper

@Singleton()
class ApiSpecificationController @Inject() (
    cc: ControllerComponents,
    apiSpecificationFetcher: ApiSpecificationFetcher
  )(implicit override val ec: ExecutionContext,
    override val mat: Materializer
  ) extends BackendController(cc)
    with StreamedResponseResourceHelper {

  def fetchApiSpecification(serviceName: ServiceName, version: ApiVersionNbr) = Action.async { implicit request =>
    OptionT(apiSpecificationFetcher.fetch(serviceName, version))
      .map(x => Ok(x))
      .getOrElse(NotFound)
  }
}
