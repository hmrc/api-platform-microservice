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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors

import org.joda.time.DateTime
import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.test.Helpers.JSON
import play.api.http.Status
import play.api.libs.json.{JsString, Json}
import uk.gov.hmrc.apiplatformmicroservice.common.builder.UserResponseBuilder
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.domain.{GetOrCreateUserIdRequest, GetOrCreateUserIdResponse, UnregisteredUserCreationRequest, UnregisteredUserResponse, UserResponse}
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.UserId
import uk.gov.hmrc.apiplatformmicroservice.util.AsyncHmrcSpec
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}

class ThirdPartyDeveloperConnectorSpec extends AsyncHmrcSpec {

  private val baseUrl = "https://example.com"
  private val jsonEncryptionKey = "jsonEncryptionKey"

  trait Setup {
    implicit val hc = HeaderCarrier()
    protected val mockHttpClient = mock[HttpClient]

    def endpoint(path: String) = s"${connector.serviceBaseUrl}/$path"

    val mockPayloadEncryption: PayloadEncryption = mock[PayloadEncryption]
    val encryptedJson = new EncryptedJson(mockPayloadEncryption)
    val config = ThirdPartyDeveloperConnector.Config(baseUrl, jsonEncryptionKey)
    val connector = new ThirdPartyDeveloperConnector(config, mockHttpClient, encryptedJson)

    val encryptedString: JsString = JsString("someEncryptedStringOfData")
    val encryptedBody = SecretRequest(encryptedString.as[String])
    when(mockPayloadEncryption.encrypt(*)(*)).thenReturn(encryptedString)

    def whenPostUnregisteredDeveloperThenReturn(r: UnregisteredUserResponse) = 
      when(mockHttpClient.POST[SecretRequest, UnregisteredUserResponse](eqTo(endpoint("unregistered-developer")), eqTo(encryptedBody), eqTo(Seq("Content-Type" -> "application/json")))(*, *, *, *))
      .thenReturn(successful(r))

    def whenPostUnregisteredDeveloperThenFail(t: Throwable) = 
      when(mockHttpClient.POST[SecretRequest, UnregisteredUserResponse](eqTo(endpoint("unregistered-developer")), eqTo(encryptedBody), eqTo(Seq("Content-Type" -> "application/json")))(*, *, *, *))
      .thenReturn(failed(t))
  }

  trait UserResponseSetup extends Setup with UserResponseBuilder {
    val userId = UserId.random
    val admin1UserId = UserId.random
    val admin2UserId = UserId.random
    val userEmail = "user@example.com"
    val admin1Email = "admin1@example.com"
    val admin2Email = "admin2@example.com"
    val emailsToFetch = Set(admin1Email, admin2Email)
    val verifiedUserResponse = buildUserResponse(userId, userEmail)
    val admin1UserResponse = buildUserResponse(admin1UserId, admin1Email)
    val admin2UserResponse = buildUserResponse(admin2UserId, admin2Email)

  }

  "createUnregisteredUser" should {
    val unregisteredUserEmail = "unregistered@example.com"
    val userId = UserId.random
    val unregisteredUserResponse = UnregisteredUserResponse(email = unregisteredUserEmail, DateTime.now, userId)


    "successfully create an unregistered user" in new Setup {
      whenPostUnregisteredDeveloperThenReturn(unregisteredUserResponse)

      val result: UnregisteredUserResponse = await(connector.createUnregisteredUser(unregisteredUserEmail))

      result.userId shouldBe userId
      result.email shouldBe unregisteredUserEmail
      verify(mockPayloadEncryption).encrypt(eqTo(Json.toJson(UnregisteredUserCreationRequest(unregisteredUserEmail))))(*)
    }

    "propagate error when the request fails" in new Setup {
      whenPostUnregisteredDeveloperThenFail(UpstreamErrorResponse("Internal server error", Status.INTERNAL_SERVER_ERROR, Status.INTERNAL_SERVER_ERROR))

      intercept[UpstreamErrorResponse] {
        await(connector.createUnregisteredUser(unregisteredUserEmail))
      }
    }
  }

  "fetchByEmails" should {
    "return the correct UserResponse" in new UserResponseSetup {
      when(mockHttpClient.GET[Seq[UserResponse]](eqTo(endpoint("developers")), eqTo(Seq("emails" -> emailsToFetch.mkString(","))))(*, *, *))
        .thenReturn(Future.successful(Seq(admin1UserResponse, admin2UserResponse)))

      await(connector.fetchByEmails(emailsToFetch)) shouldBe Seq(admin1UserResponse, admin2UserResponse)

    }

    "propagate error when the request fails" in new UserResponseSetup {
      when(mockHttpClient.GET[Seq[UserResponse]](eqTo(endpoint("developers")), eqTo(Seq("emails" -> emailsToFetch.mkString(","))))(*, *, *))
        .thenReturn(failed(UpstreamErrorResponse("Internal server error", Status.INTERNAL_SERVER_ERROR, Status.INTERNAL_SERVER_ERROR)))

      intercept[UpstreamErrorResponse] {
        await(connector.fetchByEmails(emailsToFetch))
      }
    }
  }

  "fetchDeveloper" should {
    "return the correct UserResponse" in new UserResponseSetup {
      when(mockHttpClient.GET[Option[UserResponse]](eqTo(endpoint("developer")), eqTo(Seq("email" -> userEmail)))(*, *, *))
        .thenReturn(Future.successful(Some(verifiedUserResponse)))

      await(connector.fetchDeveloper(userEmail)) shouldBe Some(verifiedUserResponse)

    }

    "propagate error when the request fails" in new UserResponseSetup {
      when(mockHttpClient.GET[Option[UserResponse]](eqTo(endpoint("developer")), eqTo(Seq("email" -> userEmail)))(*, *, *))
        .thenReturn(failed(UpstreamErrorResponse("Internal server error", Status.INTERNAL_SERVER_ERROR, Status.INTERNAL_SERVER_ERROR)))

      intercept[UpstreamErrorResponse] {
        await(connector.fetchDeveloper(userEmail))
      }
    }
  }

  "getOrCreateUserId" should {
    "return success" in new UserResponseSetup {
      val getOrCreateUserIdRequest = GetOrCreateUserIdRequest(userEmail)
      val getOrCreateUserIdResponse = GetOrCreateUserIdResponse(userId)

      when(
        mockHttpClient
          .POST[GetOrCreateUserIdRequest, GetOrCreateUserIdResponse](eqTo(endpoint("developers/user-id")), eqTo(getOrCreateUserIdRequest), eqTo(Seq(CONTENT_TYPE -> JSON)))(*, *, *, *))
          .thenReturn(Future.successful(getOrCreateUserIdResponse))

      await(connector.getOrCreateUserId(getOrCreateUserIdRequest)) shouldBe getOrCreateUserIdResponse
    }
  }
}
