/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.controllers

import uk.gov.hmrc.apiplatformmicroservice.utils._
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.SubscriptionFieldsMock
import play.api.http.HeaderNames._
import play.api.http.MimeTypes._
import play.api.http.Status._
import play.api.libs.ws.WSClient
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.controllers.SubscriptionFieldsController.SubscriptionFieldsRequest

class SubscriptionFieldsControllerSpec extends WireMockSpec with SubscriptionFieldsMock with ApiIdentifierFixtures {
  
  "SubsFieldsController" should {
    val wsClient = app.injector.instanceOf[WSClient]

    "post resulting in not found in back end" in {
      mockUpsertNotFound(Environment.PRODUCTION)
      
      val response = await(wsClient.url(s"$baseUrl/field/application/${ClientId.random}/context/$apiContextOne/version/$apiVersionNbrOne")
        .withHttpHeaders(ACCEPT -> JSON)
        .post("""{ "fields": {} }"""))

      response.status shouldBe NOT_FOUND
    }
  }
}
