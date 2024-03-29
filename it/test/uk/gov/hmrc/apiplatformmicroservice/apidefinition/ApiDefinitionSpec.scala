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
import play.api.libs.json._
import play.api.libs.ws.WSClient

import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, _}
import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.subscriptionfields.SubscriptionFieldValuesMock
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.ApplicationMock
import uk.gov.hmrc.apiplatformmicroservice.utils._

class ApiDefinitionSpec extends WireMockSpec with ApplicationMock with ApiDefinitionMock with SubscriptionFieldValuesMock {

  "WireMock" should {
    val wsClient = app.injector.instanceOf[WSClient]

    "stub get request for fetch api definitions" in {
      val applicationId = ApplicationId.random
      val clientId      = ClientId.random

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
      val versionKeys = defn.versions.keys

      versionKeys should contain(ApiVersionNbr("3.0"))
      versionKeys should contain(ApiVersionNbr("2.5rc"))
      versionKeys should contain(ApiVersionNbr("2.0"))
      versionKeys should contain(ApiVersionNbr("1.0"))

      versionKeys shouldNot contain(ApiVersionNbr("4.0"))
    }
  }
}
