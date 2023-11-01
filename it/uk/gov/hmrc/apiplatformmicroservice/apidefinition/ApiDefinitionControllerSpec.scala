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

import java.{util => ju}

import play.api.libs.json._
import play.api.http.HeaderNames._
import play.api.http.MimeTypes._
import play.api.http.Status._
import play.api.libs.ws.WSClient

import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.ApplicationMock
import uk.gov.hmrc.apiplatformmicroservice.subscriptionfields.SubscriptionFieldValuesMock
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ClientId, Environment}
import uk.gov.hmrc.apiplatformmicroservice.utils._
import uk.gov.hmrc.apiplatform.modules.apis.domain.models._

class ApiDefinitionControllerSpec extends WireMockSpec with ApplicationMock with ApiDefinitionMock with SubscriptionFieldValuesMock {

  "WireMock" should {
    val wsClient = app.injector.instanceOf[WSClient]

    "stub get request for fetch restricted subscribable apis" in {
      val applicationId = ApplicationId.random
      val clientId      = ClientId(ju.UUID.randomUUID.toString)

      mockFetchApplication(Environment.PRODUCTION, applicationId, clientId)
      mockFetchApplicationSubscriptions(Environment.PRODUCTION, applicationId)
      mockBulkFetchFieldValuesAndDefinitions(Environment.PRODUCTION, clientId)
      mockFetchApiDefinition(Environment.PRODUCTION)

      val response = await(wsClient.url(s"$baseUrl/api-definitions")
        .withQueryStringParameters("applicationId" -> applicationId.value.toString)
        .withHttpHeaders(ACCEPT -> JSON)
        .get())

      response.status shouldBe OK
      val result = Json.parse(response.body).validate[List[ApiDefinition]] match {
        case JsSuccess(v, _) => v
        case e: JsError      => fail(s"Bad response $e")
      }

      result should not be empty
      withClue("No RETIRED status allowed: ") { result.exists(_.versions.values.exists(v => v.status == ApiStatus.RETIRED)) shouldBe false }
      withClue("No Requires Trust allowed: ") { result.exists(_.requiresTrust) shouldBe false }

      val defn        = result.find(_.context == ApiContext("hello")).value
      val versionKeys = defn.versions.keys.toList

      versionKeys should contain(ApiVersionNbr("3.0"))
      versionKeys should contain(ApiVersionNbr("2.5rc"))
      versionKeys should contain(ApiVersionNbr("2.0"))
      versionKeys should contain(ApiVersionNbr("1.0"))

      versionKeys shouldNot contain(ApiVersionNbr("4.0"))
      versionKeys shouldNot contain(ApiVersionNbr("5.0"))
    }

    "stub get request for fetch unrestricted subscribable apis" in {
      val applicationId = ApplicationId.random

      mockFetchApplication(Environment.PRODUCTION, applicationId)
      mockFetchApiDefinition(Environment.PRODUCTION)

      val response = await(wsClient.url(s"$baseUrl/api-definitions")
        .withQueryStringParameters("applicationId" -> applicationId.value.toString(), "restricted" -> "false")
        .withHttpHeaders(ACCEPT -> JSON)
        .get())

      response.status shouldBe OK
      val result = Json.parse(response.body).validate[List[ApiDefinition]] match {
        case JsSuccess(v, _) => v
        case e: JsError      => fail(s"Bad response $e")
      }

      result should not be empty
      withClue("No RETIRED status allowed: ") { result.exists(_.versions.values.exists(v => v.status == ApiStatus.RETIRED)) shouldBe false }
      withClue("No Requires Trust allowed: ") { result.exists(_.requiresTrust) shouldBe false }

      val defn        = result.find(_.context == ApiContext("hello")).value
      val versionKeys = defn.versions.keys

      versionKeys should contain(ApiVersionNbr("3.0"))
      versionKeys should contain(ApiVersionNbr("2.5rc"))
      versionKeys should contain(ApiVersionNbr("2.0"))
      versionKeys should contain(ApiVersionNbr("1.0"))

      withClue("Should return deprecated versions when unrestricted") { versionKeys should contain(ApiVersionNbr("4.0")) }
      withClue("Should return alpha versions when unrestricted") { versionKeys should contain(ApiVersionNbr("5.0")) }
    }

    "stub get request for fetch open access apis" in {
      mockFetchApiDefinition(Environment.PRODUCTION)

      val response = await(wsClient.url(s"$baseUrl/api-definitions/open")
        .withQueryStringParameters("environment" -> "PRODUCTION")
        .withHttpHeaders(ACCEPT -> JSON)
        .get())

      response.status shouldBe OK
      val result = Json.parse(response.body).validate[List[ApiDefinition]] match {
        case JsSuccess(v, _) => v
        case e: JsError      => fail(s"Bad response $e")
      }

      result should not be empty

      val keys = result.map(_.context)
      keys should contain(ApiContext("another"))
      keys should contain(ApiContext("trusted"))
      keys shouldNot contain(ApiContext("hello"))
    }

    "stub get request for fetch non open access apis" in {
      mockFetchApiDefinition(Environment.PRODUCTION, "USER")

      val response = await(wsClient.url(s"$baseUrl/api-definitions/nonopen")
        .withQueryStringParameters("environment" -> "PRODUCTION")
        .withHttpHeaders(ACCEPT -> JSON)
        .get())

      response.status shouldBe OK
      val result = Json.parse(response.body).validate[List[ApiDefinition]] match {
        case JsSuccess(v, _) => v
        case e: JsError      => fail(s"Bad response $e")
      }

      result should not be empty

      val keys = result.map(_.context)
      keys should contain(ApiContext("another"))
      keys shouldNot contain(ApiContext("trusted"))
      keys should contain(ApiContext("hello"))
    }

    "stub get request for all api definitions" in {
      mockFetchApiDefinition(Environment.SANDBOX)

      val response = await(wsClient.url(s"$baseUrl/api-definitions/all")
        .withQueryStringParameters("environment" -> "SANDBOX")
        .withHttpHeaders(ACCEPT -> JSON)
        .get())

      response.status shouldBe OK

      val result = Json.parse(response.body).validate[List[ApiDefinition]] match {
        case JsSuccess(v, _) => v
        case e: JsError      => fail(s"Bad response $e")
      }

      result.find(_.context == ApiContext("trusted")).value.categories.size shouldBe 1
      result.find(_.context == ApiContext("trusted")).value.categories(0) shouldBe ApiCategory.EXAMPLE
    }
  }
}
