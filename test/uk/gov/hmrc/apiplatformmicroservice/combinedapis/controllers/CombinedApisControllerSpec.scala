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

package uk.gov.hmrc.apiplatformmicroservice.combinedapis.controllers

import org.mockito.stubbing.ScalaOngoingStubbing
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers.{contentAsString, status}
import play.api.test.{FakeRequest, StubControllerComponentsFactory}
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiCategory
import uk.gov.hmrc.apiplatformmicroservice.combinedapis.models.ApiType.{REST_API, XML_API}
import uk.gov.hmrc.apiplatformmicroservice.combinedapis.models.{BasicCombinedApiJsonFormatters, CombinedApi}
import uk.gov.hmrc.apiplatformmicroservice.combinedapis.services.CombinedApisService
import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.DeveloperIdentifier

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CombinedApisControllerSpec extends AsyncHmrcSpec with StubControllerComponentsFactory with BasicCombinedApiJsonFormatters{

  trait SetUp {
    val developerId = DeveloperIdentifier(UUID.randomUUID().toString)
    val mockCombinedApisService = mock[CombinedApisService]
    val objInTest = new CombinedApisController(mockCombinedApisService, stubControllerComponents())
    val combinedApis = List(CombinedApi("restService1", List(ApiCategory("VAT")), REST_API), CombinedApi("xmlService1", List(ApiCategory("OTHER")), XML_API))

    def primeCombinedApisService(apis: List[CombinedApi]): ScalaOngoingStubbing[Future[List[CombinedApi]]] ={
      when(mockCombinedApisService.fetchCombinedApisForDeveloperId(*)(*)).thenReturn(Future.successful(apis))
    }
  }


  "CombinedApisController" should {
    "return 200 and apis when service returns apis" in new SetUp {
      primeCombinedApisService(combinedApis)

      val result: Future[Result] = objInTest.getCombinedApisForDeveloper(developerId)(FakeRequest())
      status(result) shouldBe 200
      contentAsString(result) shouldBe Json.toJson(combinedApis).toString()
    }

  }
}
