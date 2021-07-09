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

import uk.gov.hmrc.apiplatformmicroservice.common.ProxiedHttpClient
import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import play.api.http.Status._
import scala.concurrent.ExecutionContext.Implicits.global
import com.github.tomakehurst.wiremock.client.WireMock._

import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.apiplatformmicroservice.common.utils.{WireMockSugarExtensions}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.Environment.{PRODUCTION, SANDBOX}
import uk.gov.hmrc.apiplatformmicroservice.utils.PrincipalAndSubordinateWireMockSetup
import uk.gov.hmrc.apiplatformmicroservice.utils.ConfigBuilder
import uk.gov.hmrc.apiplatformmicroservice.common.builder._
import uk.gov.hmrc.http.UpstreamErrorResponse
import AbstractThirdPartyApplicationConnector._
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models._
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.UuidIdentifier
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.EmailIdentifier
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.UserId
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.Role
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.domain.{AddCollaboratorToTpaRequest,AddCollaboratorToTpaResponse}

class ThirdPartyApplicationConnectorSpec
    extends AsyncHmrcSpec 
    with WireMockSugarExtensions 
    with GuiceOneServerPerSuite
    with ConfigBuilder
    with PrincipalAndSubordinateWireMockSetup
    with ApplicationBuilder {

  private val helloWorldContext = ApiContext("hello-world")
  private val versionOne = ApiVersion("1.0")
  private val versionTwo = ApiVersion("2.0")


  private val applicationIdOne = ApplicationId.random
  private val applicationIdTwo = ApplicationId.random

  trait Setup {
    implicit val applicationResponseWrites = Json.writes[ApplicationResponse]

    implicit val hc = HeaderCarrier()
    val httpClient = app.injector.instanceOf[HttpClient]
    protected val mockProxiedHttpClient = mock[ProxiedHttpClient]
    val apiKeyTest = "5bb51bca-8f97-4f2b-aee4-81a4a70a42d3"
    val bearer = "TestBearerToken"


    val config = AbstractThirdPartyApplicationConnector.Config(
      applicationBaseUrl = s"http://$WireMockHost:$WireMockPrincipalPort",
      applicationUseProxy = false,
      applicationBearerToken = bearer,
      applicationApiKey = apiKeyTest
    )
    val connector: AbstractThirdPartyApplicationConnector = new PrincipalThirdPartyApplicationConnector(config, httpClient, mockProxiedHttpClient)
  }

  trait SubordinateSetup extends Setup {
    override val config = AbstractThirdPartyApplicationConnector.Config(
      applicationBaseUrl = s"http://$WireMockHost:$WireMockSubordinatePort",
      applicationUseProxy = false,
      applicationBearerToken = bearer,
      applicationApiKey = apiKeyTest
    )
    override val connector = new SubordinateThirdPartyApplicationConnector(config, httpClient, mockProxiedHttpClient)
  }

  "fetchApplications for a collaborator by email" should {
    val email = EmailIdentifier("email@example.com")
    val url = "/application"
    val applicationResponses = List(ApplicationResponse(applicationIdOne), ApplicationResponse(applicationIdTwo))

    
    "return application Ids" in new Setup {
      stubFor(PRODUCTION)(
        get(urlPathEqualTo(url))
        .withQueryParam("emailAddress", equalTo(email.email))
        .willReturn(
          aResponse()
          .withStatus(OK)
          .withJsonBody(applicationResponses)
        )
      )
      val result = await(connector.fetchApplications(email))

      result.size shouldBe 2
      result should contain allOf (applicationIdOne, applicationIdTwo)
    }

    "propagate error when endpoint returns error" in new Setup {
      stubFor(PRODUCTION)(
        get(urlPathEqualTo(url))
        .withQueryParam("emailAddress", equalTo(email.email))
        .willReturn(
          aResponse()
          .withStatus(NOT_FOUND)
        )
      )
      intercept[UpstreamErrorResponse] {
        await(connector.fetchApplications(email))
      }.statusCode shouldBe NOT_FOUND
    }
  }

  "fetchApplications for a collaborator by user id" should {
    val userId = UuidIdentifier(UserId.random)
    val url = s"/developer/${userId.userId.value}/applications"
    val applicationResponses = List(ApplicationResponse(applicationIdOne), ApplicationResponse(applicationIdTwo))

    "return application Ids" in new Setup {
      stubFor(PRODUCTION)(
        get(urlPathEqualTo(url))
        .willReturn(
          aResponse()
          .withStatus(OK)
          .withJsonBody(applicationResponses)
        )
      )

      val result = await(connector.fetchApplications(userId))

      result.size shouldBe 2
      result should contain allOf (applicationIdOne, applicationIdTwo)
    }

    "propagate error when endpoint returns error" in new Setup {
      stubFor(PRODUCTION)(
        get(urlPathEqualTo(url))
        .willReturn(
          aResponse()
          .withStatus(NOT_FOUND)
        )
      )

      intercept[UpstreamErrorResponse] {
        await(connector.fetchApplications(userId))
      }.statusCode shouldBe NOT_FOUND
    }
  }
  
  import ApiDefinitionJsonFormatters._

  "fetchSubscriptions for a collaborator by userId" should {
    val userId = UuidIdentifier(UserId.random)
    val url = s"/developer/${userId.userId.value}/subscriptions"

    val expectedSubscriptions = Seq(ApiIdentifier(helloWorldContext, versionOne), ApiIdentifier(helloWorldContext, versionTwo))

    "return subscriptions" in new Setup {
      stubFor(PRODUCTION)(
        get(urlEqualTo(url))
        .willReturn(
          aResponse()
          .withStatus(OK)
          .withJsonBody(expectedSubscriptions)
        )
      )

      val result = await(connector.fetchSubscriptions(userId))

      result shouldBe expectedSubscriptions
    }

    "propagate error when endpoint returns error" in new Setup {
      stubFor(PRODUCTION)(
        get(urlEqualTo(url))
        .willReturn(
          aResponse()
          .withStatus(NOT_FOUND)
        )
      )

      intercept[UpstreamErrorResponse] {
        await(connector.fetchSubscriptions(userId))
      }.statusCode shouldBe NOT_FOUND
    }
  }

  "fetchApplication" should {
    val applicationId = ApplicationId.random
    val url = s"/application/${applicationId.value}"
    import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.services.ApplicationJsonFormatters._

    "propagate error when endpoint returns error" in new Setup {
      stubFor(PRODUCTION)(
        get(urlEqualTo(url))
        .willReturn(
          aResponse()
          .withStatus(INTERNAL_SERVER_ERROR)
        )
      )
      intercept[UpstreamErrorResponse] {
        await(connector.fetchApplication(applicationId))
      }.statusCode shouldBe INTERNAL_SERVER_ERROR
    }

    "return None when appropriate" in new Setup {
      stubFor(PRODUCTION)(
        get(urlEqualTo(url))
        .willReturn(
          aResponse()
          .withStatus(NOT_FOUND)
        )
      )
      await(connector.fetchApplication(applicationId)) shouldBe None
    }

    "return the application" in new Setup {
      val application = buildApplication(applicationId)

      stubFor(PRODUCTION)(
        get(urlEqualTo(url))
        .willReturn(
          aResponse()
          .withStatus(OK)
          .withJsonBody(application)
        )
      )

      await(connector.fetchApplication(applicationId)) shouldBe Some(application)
    }
  }

  "fetchSubscriptions" should {
    import AbstractThirdPartyApplicationConnector._
    import SubscriptionsHelper._
    val applicationId = ApplicationId.random
    val url = s"/application/${applicationId.value}/subscription"

    "propagate error when endpoint returns 5xx error" in new Setup {
      stubFor(PRODUCTION)(
        get(urlEqualTo(url))
        .willReturn(
          aResponse()
          .withStatus(INTERNAL_SERVER_ERROR)
        )
      )
      intercept[UpstreamErrorResponse] {
        await(connector.fetchSubscriptionsById(applicationId))
      }.statusCode shouldBe INTERNAL_SERVER_ERROR
    }

    "handle 5xx from subordinate" in new SubordinateSetup {
      stubFor(SANDBOX)(
        get(urlEqualTo(url))
        .willReturn(
          aResponse()
          .withStatus(INTERNAL_SERVER_ERROR)
        )
      )

      await(connector.fetchSubscriptionsById(applicationId)) shouldBe Set.empty
    }

    "handle Not Found" in new Setup {
      stubFor(PRODUCTION)(
        get(urlEqualTo(url))
        .willReturn(
          aResponse()
          .withStatus(NOT_FOUND)
        )
      )
      intercept[ApplicationNotFound] {
        await(connector.fetchSubscriptionsById(applicationId))
      }
    }

    "return None when appropriate" in new Setup {
      stubFor(PRODUCTION)(
        get(urlEqualTo(url))
        .willReturn(
          aResponse()
          .withStatus(OK)
          .withJsonBody(Set.empty[ApiIdentifier])
        )
      )

      await(connector.fetchSubscriptionsById(applicationId)) shouldBe Set.empty
    }

    "return the subscription versions that are subscribed to" in new Setup {
      stubFor(PRODUCTION)(
        get(urlEqualTo(url))
        .willReturn(
          aResponse()
          .withStatus(OK)
          .withJsonBody(MixedSubscriptions)
        )
      )

      await(connector.fetchSubscriptionsById(applicationId)) shouldBe Set(
        ApiIdentifier(ContextA, VersionOne),
        ApiIdentifier(ContextB, VersionTwo)
      )
    }
  }

  "subscribeToApi" should {
    import SubscriptionsHelper._

    val apiId = ApiIdentifier(ContextA, VersionOne)
    val applicationId = ApplicationId.random
    val url = s"/application/${applicationId.value}/subscription"

    "return the success when everything works" in new Setup {
      stubFor(PRODUCTION)(
        post(urlEqualTo(url))
        .withJsonRequestBody(apiId)
        .willReturn(
          aResponse()
          .withStatus(OK)
        )
      )
      await(connector.subscribeToApi(applicationId, apiId)) shouldBe SubscriptionUpdateSuccessResult
    }
  }

  trait CollaboratorSetup extends Setup with CollaboratorsBuilder {
    val applicationId = ApplicationId.random
    val requestorEmail = "requestor@example.com"
    val newTeamMemberEmail = "newTeamMember@example.com"
    val adminsToEmail = Set("bobby@example.com", "daisy@example.com")
    val newCollaborator = buildCollaborator(newTeamMemberEmail, Role.ADMINISTRATOR)
    val addCollaboratorRequest = AddCollaboratorToTpaRequest(requestorEmail, newCollaborator, isRegistered = true, adminsToEmail)
    val url = s"/application/${applicationId.value}/collaborator"
  }

  "addCollaborator" should {
    "return success" in new CollaboratorSetup {
      val addCollaboratorResponse = AddCollaboratorToTpaResponse(true)

      stubFor(PRODUCTION)(
        post(urlEqualTo(url))
        .withJsonRequestBody(addCollaboratorRequest)
        .willReturn(
          aResponse()
          .withStatus(OK)
          .withJsonBody(addCollaboratorResponse)
        )
      )
      val result: AddCollaboratorResult = await(connector.addCollaborator(applicationId, addCollaboratorRequest))

      result shouldEqual AddCollaboratorSuccessResult(addCollaboratorResponse.registeredUser)
    }

    "return teamMember already exists response" in new CollaboratorSetup {
      stubFor(PRODUCTION)(
        post(urlEqualTo(url))
        .withJsonRequestBody(addCollaboratorRequest)
        .willReturn(
          aResponse()
          .withStatus(CONFLICT)
        )
      )
      val result: AddCollaboratorResult = await(connector.addCollaborator(applicationId, addCollaboratorRequest))

      result shouldEqual CollaboratorAlreadyExistsFailureResult
    }

    "return application not found response" in new CollaboratorSetup {
      stubFor(PRODUCTION)(
        post(urlEqualTo(url))
        .withJsonRequestBody(addCollaboratorRequest)
        .willReturn(
          aResponse()
          .withStatus(NOT_FOUND)
        )
      )

      intercept[ApplicationNotFound] {
        await(connector.addCollaborator(applicationId, addCollaboratorRequest))
      }
    }
  }
}