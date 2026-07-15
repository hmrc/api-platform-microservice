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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication

import play.api.http.HeaderNames.*
import play.api.http.MimeTypes.*
import play.api.http.Status.*
import play.api.libs.ws.WSClient

import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, Environment}
import uk.gov.hmrc.apiplatformmicroservice.utils.WireMockSpec

class ThirdPartyApplicationSpec extends WireMockSpec with ApplicationMock {

  "WireMock" should {
    val wsClient = app.injector.instanceOf[WSClient]

    "stub get request for fetching an application" in {
      val applicationId = ApplicationId.random
      mockFetchApplicationWithFieldsNotFound(Environment.Sandbox, applicationId)
      mockFetchApplicationWithFields(Environment.Production, applicationId)

      val response = await(wsClient.url(s"$baseUrl/applications/$applicationId")
        .withHttpHeaders(ACCEPT -> JSON)
        .get())

      response.status shouldBe OK
    }
  }
}
