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

import play.api.libs.json.Json
import play.api.test._
import play.api.test.Helpers._
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.mocks.{ApiCategoryDetailsFetcherModule, XmlApisConnectorMockingHelper}
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiDefinitionJsonFormatters._
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.{ApiCategory, ApiCategoryDetails, ApiDefinitionTestDataHelper}
import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.http.HeaderCarrier
import akka.stream.testkit.NoMaterializer
import uk.gov.hmrc.apiplatformmicroservice.xmlapis.models.XmlApi

import scala.concurrent.ExecutionContext.Implicits.global

class ApiCategoriesControllerSpec extends AsyncHmrcSpec with ApiDefinitionTestDataHelper {

  trait Setup extends ApiCategoryDetailsFetcherModule with XmlApisConnectorMockingHelper {
    implicit val headerCarrier = HeaderCarrier()
    implicit val mat           = NoMaterializer

    val controller = new ApiCategoriesController(Helpers.stubControllerComponents(), ApiCategoryDetailsForCollaboratorFetcherMock.aMock, XmlApisConnectorMock.aMock)

    val category1    = ApiCategoryDetails("API_CATEGORY_1", "API Category 1")
    val category2    = ApiCategoryDetails("API_CATEGORY_2", "API Category 2")
    val xmlCategory1 = ApiCategoryDetails("VAT", "VAT")
    val xmlCategory2 = ApiCategoryDetails("NEW_CATEGORY", "NEW_CATEGORY")
    val xmlApi1      = XmlApi("name", "serviceName", "context", "description", Some(List(ApiCategory("VAT"), ApiCategory("NEW_CATEGORY"))))
  }

  "fetchAllApiCategories" should {
    "return all valid API Categories" in new Setup {

      ApiCategoryDetailsForCollaboratorFetcherMock.willReturnApiCategoryDetails(category1, category2)
      XmlApisConnectorMock.willReturnAllXmlApis(xmlApi1)

      val result = controller.fetchAllAPICategories()(FakeRequest())

      status(result) must be(OK)

      val parsedCategories: List[ApiCategoryDetails] = Json.fromJson[List[ApiCategoryDetails]](contentAsJson(result)).get
      parsedCategories.size must be(4)
      parsedCategories must contain only (category1, category2, xmlCategory1, xmlCategory2)
    }
  }
}
