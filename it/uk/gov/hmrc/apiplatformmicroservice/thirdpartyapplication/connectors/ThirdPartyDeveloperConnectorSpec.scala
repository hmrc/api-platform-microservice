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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors

import uk.gov.hmrc.apiplatformmicroservice.common.utils.{AsyncHmrcSpec, WireMockSugar, WireMockSugarExtensions}
import uk.gov.hmrc.apiplatformmicroservice.common.builder.UserResponseBuilder
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import uk.gov.hmrc.http.HttpClient
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.http.HeaderCarrier
import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.Status._
import uk.gov.hmrc.apiplatform.modules.common.domain.models.UserId
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.domain._
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax

class ThirdPartyDeveloperConnectorSpec
    extends AsyncHmrcSpec
    with WireMockSugar
    with WireMockSugarExtensions
    with GuiceOneServerPerSuite
    with UserResponseBuilder {

  trait Setup {
    implicit val hc = HeaderCarrier()

    val email1  = "fakeemail1".toLaxEmail
    val email2  = "fakeemail2".toLaxEmail
    val userId1 = UserId.random
    val userId2 = UserId.random

    val httpClient        = app.injector.instanceOf[HttpClient]
    val mockEncryptedJson = mock[EncryptedJson]

    val mockConfig: ThirdPartyDeveloperConnector.Config = mock[ThirdPartyDeveloperConnector.Config]
    when(mockConfig.applicationBaseUrl).thenReturn(wireMockUrl)

    val tpdConnector = new ThirdPartyDeveloperConnector(mockConfig, httpClient, mockEncryptedJson)
  }

  "fetchByEmails" should {
    val url = "/developers/get-by-emails"

    "respond with 200 and data" in new Setup {
      val fakeUser1 = buildUserResponse(userId1, email1, true)
      val fakeUser2 = buildUserResponse(userId2, email2, true)

      stubFor(
        post(urlEqualTo(url))
          .withJsonRequestBody(List(email1, email2))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withJsonBody(List(fakeUser1, fakeUser2))
          )
      )

      val result = await(tpdConnector.fetchByEmails(Set(email1, email2)))

      result.toList should contain.allOf(fakeUser1, fakeUser2)
    }

    "respond with BAD_REQUEST when no email addresses provided" in new Setup {
      stubFor(
        post(urlEqualTo(url))
          .withJsonRequestBody(List.empty[String])
          .willReturn(
            aResponse()
              .withStatus(BAD_REQUEST)
          )
      )

      intercept[UpstreamErrorResponse] {
        await(tpdConnector.fetchByEmails(Set.empty))
      }.statusCode shouldBe BAD_REQUEST
    }
  }

  "getOrCreateUserId" should {
    val url = "/developers/user-id"
    "return success for email" in new Setup {
      val getOrCreateUserIdRequest  = GetOrCreateUserIdRequest(email1)
      val getOrCreateUserIdResponse = GetOrCreateUserIdResponse(userId1)

      stubFor(
        post(urlEqualTo(url))
          .withJsonRequestBody(getOrCreateUserIdRequest)
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withJsonBody(getOrCreateUserIdResponse)
          )
      )

      await(tpdConnector.getOrCreateUserId(getOrCreateUserIdRequest)) shouldBe getOrCreateUserIdResponse
    }
  }
}
