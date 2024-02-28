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

import play.api.http.HeaderNames._
import play.api.http.MimeTypes._
import play.api.http.Status._
import play.api.libs.json._
import play.api.libs.ws.WSClient

import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ClientId, Environment, _}
import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.common.builder.DefinitionsFromJson
import uk.gov.hmrc.apiplatformmicroservice.subscriptionfields.SubscriptionFieldValuesMock
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.ApplicationMock
import uk.gov.hmrc.apiplatformmicroservice.utils._

class ApiDefinitionControllerSpec extends WireMockSpec with ApplicationMock with ApiDefinitionMock with SubscriptionFieldValuesMock with DefinitionsFromJson {

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
      keys should contain(ApiContext("hello"))
    }
  }

  "get single api definition" should {
    val wsClient = app.injector.instanceOf[WSClient]

    "from only production" in {
      val serviceName = ServiceName("hello-world")
      val definition  = apiDefinition("Hello World")

      whenGetDefinition(Environment.PRODUCTION)(serviceName, definition)
      implicit val formatter: OFormat[Locator[ApiDefinition]] = Locator.buildLocatorFormatter[ApiDefinition]

      val response = await(wsClient.url(s"$baseUrl/api-definitions/service-name/$serviceName")
        .withHttpHeaders(ACCEPT -> JSON)
        .get())

      response.status shouldBe OK

      val result = Json.parse(response.body).validate[Locator[ApiDefinition]] match {
        case JsSuccess(v, _) => v
        case e: JsError      => fail(s"Bad response $e")
      }
      result shouldBe Locator.Production(definition)
    }

    "from only sandbox" in {
      val serviceName = ServiceName("hello-world")
      val definition  = apiDefinition("Hello World")

      whenGetDefinition(Environment.SANDBOX)(serviceName, definition)
      implicit val formatter: OFormat[Locator[ApiDefinition]] = Locator.buildLocatorFormatter[ApiDefinition]

      val response = await(wsClient.url(s"$baseUrl/api-definitions/service-name/$serviceName")
        .withHttpHeaders(ACCEPT -> JSON)
        .get())

      response.status shouldBe OK

      val result = Json.parse(response.body).validate[Locator[ApiDefinition]] match {
        case JsSuccess(v, _) => v
        case e: JsError      => fail(s"Bad response $e")
      }
      result shouldBe Locator.Sandbox(definition)
    }

    "from both" in {
      val serviceName = ServiceName("hello-world")
      val definition  = apiDefinition("Hello World")

      whenGetDefinition(Environment.PRODUCTION)(serviceName, definition)
      whenGetDefinition(Environment.SANDBOX)(serviceName, definition)
      implicit val formatter: OFormat[Locator[ApiDefinition]] = Locator.buildLocatorFormatter[ApiDefinition]

      val response = await(wsClient.url(s"$baseUrl/api-definitions/service-name/$serviceName")
        .withHttpHeaders(ACCEPT -> JSON)
        .get())

      response.status shouldBe OK

      val result = Json.parse(response.body).validate[Locator[ApiDefinition]] match {
        case JsSuccess(v, _) => v
        case e: JsError      => fail(s"Bad response $e")
      }
      result shouldBe Locator.Both(definition, definition)
    }

    "from neither" in {
      val serviceName = ServiceName("hello-world")
      val response    = await(wsClient.url(s"$baseUrl/api-definitions/service-name/$serviceName")
        .withHttpHeaders(ACCEPT -> JSON)
        .get())

      response.status shouldBe NOT_FOUND

    }
  }
}
