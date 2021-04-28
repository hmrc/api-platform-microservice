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

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers.{contentAsJson, status}
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.mocks.ApiCategoryDetailsFetcherModule
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiDefinitionJsonFormatters._
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.{ApiCategoryDetails, ApiDefinitionTestDataHelper}
import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class ApiCategoriesControllerSpec extends AsyncHmrcSpec with GuiceOneAppPerSuite with ApiDefinitionTestDataHelper {

  trait Setup extends ApiCategoryDetailsFetcherModule {
    implicit val headerCarrier = HeaderCarrier()
    implicit val system = ActorSystem("test")
    implicit val mat = ActorMaterializer()

    val controller = new ApiCategoriesController(Helpers.stubControllerComponents(), ApiCategoryDetailsForCollaboratorFetcherMock.aMock)

    val category1 = ApiCategoryDetails("API_CATEGORY_1", "API Category 1")
    val category2 = ApiCategoryDetails("API_CATEGORY_2", "API Category 2")
  }

  "fetchAllApiCategories" should {
    "return all valid API Categories" in new Setup {
      ApiCategoryDetailsForCollaboratorFetcherMock.willReturnApiCategoryDetails(category1, category2)

      val result = controller.fetchAllAPICategories()(FakeRequest())

      status(result) must be (OK)

      val parsedCategories: List[ApiCategoryDetails] = Json.fromJson[List[ApiCategoryDetails]](contentAsJson(result)).get
      parsedCategories.size must be (2)
      parsedCategories must contain only (category1, category2)
    }
  }
}
