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

package uk.gov.hmrc.apiplatformmicroservice.combinedapis.controllers

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import akka.stream.Materializer

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiDefinitionJsonFormatters._
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.{ApiCategory, ApiCategoryDetails}
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.services._
import uk.gov.hmrc.apiplatformmicroservice.common.StreamedResponseResourceHelper
import uk.gov.hmrc.apiplatformmicroservice.common.controllers._
import uk.gov.hmrc.apiplatformmicroservice.xmlapis.connectors.XmlApisConnector
import uk.gov.hmrc.apiplatformmicroservice.xmlapis.models.XmlApi

@Singleton()
class ApiCategoriesController @Inject() (
    cc: ControllerComponents,
    apiCategoryDetailsFetcher: ApiCategoryDetailsFetcher,
    xmlApisConnector: XmlApisConnector
  )(implicit override val ec: ExecutionContext,
    override val mat: Materializer
  ) extends BackendController(cc) with StreamedResponseResourceHelper {

  private def getXmlCategories(xmlApi: XmlApi) = {
    extractXmlCategories(xmlApi.categories)
  }

  private def extractXmlCategories(x: Option[List[ApiCategory]]) = {
    x match {
      case Some(xmlApiList: List[ApiCategory]) => xmlApiList.map(x => ApiCategoryDetails(x.value, x.value))
      case _                                   => List.empty
    }
  }

  def fetchAllAPICategories(): Action[AnyContent] = Action.async { implicit request =>
    val r: Future[Result] = for {
      apiCategories <- apiCategoryDetailsFetcher.fetch()
      xmlApis       <- xmlApisConnector.fetchAllXmlApis()
      xmlCategories  = xmlApis.flatMap(getXmlCategories).toList
                         .filterNot(x => apiCategories.map(_.category).contains(x.category))
    } yield {
      val combinedCategories: List[ApiCategoryDetails] = xmlCategories ++ apiCategories
      Ok(Json.toJson(combinedCategories))
    }
    r recover recovery
  }

}
