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

import play.api.http.HeaderNames._
import play.api.http.MimeTypes._
import play.api.http.Status._
import play.api.libs.ws.WSClient

import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.SubscriptionFieldsMock
import uk.gov.hmrc.apiplatformmicroservice.utils._

class SubscriptionFieldsControllerSpec extends WireMockSpec with SubscriptionFieldsMock with ApiIdentifierFixtures {

  "SubscriptionFieldsController" should {
    val wsClient = app.injector.instanceOf[WSClient]

    "put resulting in NOT FOUND in back end" in {
      mockUpsertNotFound(Environment.PRODUCTION)

      val response = await(wsClient.url(s"$baseUrl/subscription-fields/field/application/xyz/context/abc/version/stu?environment=PRODUCTION")
        .withHttpHeaders(ACCEPT -> JSON, CONTENT_TYPE -> JSON)
        .put("""{ "fields": { "A" : "x" } }"""))

      response.status shouldBe NOT_FOUND
    }

    "put resulting in BAD REQUEST when back end responds with BAD REQUEST" in {
      mockUpsertWithBadRequest(Environment.PRODUCTION)

      val response = await(wsClient.url(s"$baseUrl/subscription-fields/field/application/xyz/context/abc/version/stu?environment=PRODUCTION")
        .withHttpHeaders(ACCEPT -> JSON, CONTENT_TYPE -> JSON)
        .put("""{ "fields": { "A" : "x" } }"""))

      response.status shouldBe BAD_REQUEST
    }

    "put resulting in OK when back end responds with OK" in {
      mockUpsert(Environment.PRODUCTION)

      val response = await(wsClient.url(s"$baseUrl/subscription-fields/field/application/xyz/context/abc/version/stu?environment=PRODUCTION")
        .withHttpHeaders(ACCEPT -> JSON, CONTENT_TYPE -> JSON)
        .put("""{ "fields": { "A" : "x" } }"""))

      response.status shouldBe OK
    }
  }
}
