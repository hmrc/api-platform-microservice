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

package uk.gov.hmrc.apiplatformmicroservice.apidefinition


import play.api.http.HeaderNames._
import play.api.http.MimeTypes._
import play.api.http.Status._
import play.api.libs.ws.WSClient

import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.ApplicationMock
import uk.gov.hmrc.apiplatformmicroservice.subscriptionfields.SubscriptionFieldValuesMock
import uk.gov.hmrc.apiplatformmicroservice.utils._

class ApiCategoriesControllerSpec extends WireMockSpec with ApplicationMock with ApiDefinitionMock with SubscriptionFieldValuesMock {

  "ApiCategoriesController" should {
    val wsClient = app.injector.instanceOf[WSClient]

    "stub get request for fetch restricted subscribable apis" in {

      val response = await(wsClient.url(s"$baseUrl/api-categories")
        .withHttpHeaders(ACCEPT -> JSON)
        .get())

      response.status shouldBe OK
    }

  }
}
