/*
 * Copyright 2020 HM Revenue & Customs
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

import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiIdentifier
import uk.gov.hmrc.apiplatformmicroservice.common.ProxiedHttpClient
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.AbstractThirdPartyApplicationConnector.{ApplicationNotFound, ApplicationResponse}
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatformmicroservice.util.AsyncHmrcSpec
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.{ApiContext, ApiIdentifier, ApiVersion}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.{Application, Role}
import play.api.http.Status._
import uk.gov.hmrc.apiplatformmicroservice.common.builder.CollaboratorsBuilder
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.domain.{AddCollaboratorToTpaRequest, AddCollaboratorToTpaResponse}

import scala.concurrent.Future.{failed, successful}
class ThirdPartyApplicationConnectorSpec extends AsyncHmrcSpec {

  private val helloWorldContext = ApiContext("hello-world")
  private val versionOne = ApiVersion("1.0")
  private val versionTwo = ApiVersion("2.0")

  private val applicationIdOne = ApplicationId.random
  private val applicationIdTwo = ApplicationId.random

  private val baseUrl = "https://example.com"

  class Setup(proxyEnabled: Boolean = false) {
    implicit val hc = HeaderCarrier()
    protected val mockHttpClient = mock[HttpClient]
    protected val mockProxiedHttpClient = mock[ProxiedHttpClient]
    val apiKeyTest = "5bb51bca-8f97-4f2b-aee4-81a4a70a42d3"
    val bearer = "TestBearerToken"

    val connector = new AbstractThirdPartyApplicationConnector {
      val httpClient = mockHttpClient
      val proxiedHttpClient = mockProxiedHttpClient

      val config = AbstractThirdPartyApplicationConnector.Config(
        baseUrl,
        proxyEnabled,
        bearer,
        apiKeyTest
      )
    }
  }

  class SubordinateSetup(proxyEnabled: Boolean = false) extends Setup(proxyEnabled) {

    val config = AbstractThirdPartyApplicationConnector.Config(
      baseUrl,
      proxyEnabled,
      bearer,
      apiKeyTest
    )

    override val connector = new SubordinateThirdPartyApplicationConnector(
      config,
      mockHttpClient,
      mockProxiedHttpClient
    ) {}
  }

  "http" when {
    "configured not to use the proxy" should {
      "use the HttpClient" in new Setup(proxyEnabled = false) {
        connector.http shouldBe mockHttpClient
      }
    }

    "configured to use the proxy" should {
      "use the ProxiedHttpClient with the correct authorisation" in new Setup(proxyEnabled = true) {
        when(mockProxiedHttpClient.withHeaders(bearer, apiKeyTest)).thenReturn(mockProxiedHttpClient)

        connector.http shouldBe mockProxiedHttpClient

        verify(mockProxiedHttpClient).withHeaders(bearer, apiKeyTest)
      }
    }
  }

  "fetchApplicationsByEmail" should {
    val email = "email@example.com"
    val url = baseUrl + "/application"
    val applicationResponses = List(ApplicationResponse(applicationIdOne), ApplicationResponse(applicationIdTwo))

    "return application Ids" in new Setup {
      when(mockHttpClient.GET[Seq[ApplicationResponse]](eqTo(url), eqTo(Seq("emailAddress" -> email)))(*, *, *))
        .thenReturn(successful(applicationResponses))

      val result = await(connector.fetchApplicationsByEmail(email))

      result.size shouldBe 2
      result should contain allOf (applicationIdOne, applicationIdTwo)
    }

    "propagate error when endpoint returns error" in new Setup {
      when(mockHttpClient.GET[Seq[ApplicationResponse]](eqTo(url), eqTo(Seq("emailAddress" -> email)))(*, *, *))
        .thenReturn(failed(new NotFoundException("")))

      intercept[NotFoundException] {
        await(connector.fetchApplicationsByEmail(email))
      }
    }
  }

  "fetchSubscriptionsByEmail" should {
    val email = "email@example.com"
    val url = s"$baseUrl/developer/$email/subscriptions"
    val expectedSubscriptions = Seq(ApiIdentifier(helloWorldContext, versionOne), ApiIdentifier(helloWorldContext, versionTwo))

    "return subscriptions" in new Setup {
      when(mockHttpClient.GET[Seq[ApiIdentifier]](eqTo(url))(*, *, *))
        .thenReturn(successful(expectedSubscriptions))

      val result = await(connector.fetchSubscriptionsByEmail(email))

      result shouldBe expectedSubscriptions
    }

    "propagate error when endpoint returns error" in new Setup {
      when(mockHttpClient.GET[Seq[ApiIdentifier]](eqTo(url))(*, *, *))
        .thenReturn(failed(new NotFoundException("")))

      intercept[NotFoundException] {
        await(connector.fetchSubscriptionsByEmail(email))
      }
    }
  }

  // TODO - very little purpose to these tests - replace with wiremock integration asap
  "fetchApplication" should {
    val applicationId = ApplicationId.random
    val url = s"$baseUrl/application/${applicationId.value}"

    "propagate error when endpoint returns error" in new Setup {
      when(mockHttpClient.GET[Option[Application]](eqTo(url))(*, *, *))
        .thenReturn(failed(new RuntimeException("Bang")))

      intercept[RuntimeException] {
        await(connector.fetchApplication(applicationId))
      }.getMessage() shouldBe "Bang"
    }

    "return None when appropriate" in new Setup {
      when(mockHttpClient.GET[Option[Application]](eqTo(url))(*, *, *))
        .thenReturn(successful(None))

      await(connector.fetchApplication(applicationId)) shouldBe None
    }

    "return the application" in new Setup {
      val mockApp = mock[Application]

      when(mockHttpClient.GET[Option[Application]](eqTo(url))(*, *, *))
        .thenReturn(successful(Some(mockApp)))

      await(connector.fetchApplication(applicationId)) shouldBe Some(mockApp)
    }
  }

  // TODO - very little purpose to these tests - replace with wiremock integration asap
  "fetchSubscriptions" should {
    import AbstractThirdPartyApplicationConnector._
    import SubscriptionsHelper._
    val applicationId = ApplicationId.random
    val url = s"$baseUrl/application/${applicationId.value}/subscription"

    "propagate error when endpoint returns error" in new Setup {
      when(mockHttpClient.GET[Set[ApiIdentifier]](eqTo(url))(*, *, *))
        .thenReturn(failed(new RuntimeException("Bang")))

      intercept[RuntimeException] {
        await(connector.fetchSubscriptionsById(applicationId))
      }.getMessage() shouldBe "Bang"
    }

    "handle 5xx from subordinate" in new SubordinateSetup {
      when(mockHttpClient.GET[Set[ApiIdentifier]](eqTo(url))(*, *, *))
        .thenReturn(failed(UpstreamErrorResponse.apply("Nothing here", INTERNAL_SERVER_ERROR)))

      await(connector.fetchSubscriptionsById(applicationId)) shouldBe Set.empty
    }

    "handle 5xx from principal" in new Setup {
      when(mockHttpClient.GET[Set[ApiIdentifier]](eqTo(url))(*, *, *))
        .thenReturn(failed(UpstreamErrorResponse.apply("Nothing here", INTERNAL_SERVER_ERROR)))

      intercept[UpstreamErrorResponse] {
        await(connector.fetchSubscriptionsById(applicationId))
      }
    }

    "handle Not Found" in new Setup {
      when(mockHttpClient.GET[Set[ApiIdentifier]](eqTo(url))(*, *, *)).thenReturn(failed(UpstreamErrorResponse.apply("Nothing here", NOT_FOUND)))

      intercept[ApplicationNotFound] {
        await(connector.fetchSubscriptionsById(applicationId))
      }
    }

    "return None when appropriate" in new Setup {
      when(mockHttpClient.GET[Set[ApiIdentifier]](eqTo(url))(*, *, *))
        .thenReturn(successful(Set.empty))

      await(connector.fetchSubscriptionsById(applicationId)) shouldBe Set.empty
    }

    "return the subscription versions that are subscribed to" in new Setup {
      when(mockHttpClient.GET[Set[ApiIdentifier]](eqTo(url))(*, *, *))
        .thenReturn(successful(MixedSubscriptions))

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
    val url = s"$baseUrl/application/${applicationId.value}/subscription"

    "return the success when everything works" in new Setup {
      when(mockHttpClient.POST[ApiIdentifier, Either[UpstreamErrorResponse, Unit]](eqTo(url), eqTo(apiId), *)(*,*,*,*))
        .thenReturn(successful(Right(())))

      await(connector.subscribeToApi(applicationId, apiId)) shouldBe SubscriptionUpdateSuccessResult
    }
  }

  class CollaboratorSetup extends Setup with CollaboratorsBuilder {
    val applicationId = ApplicationId.random
    val requestorEmail = "requestor@example.com"
    val newTeamMemberEmail = "newTeamMember@example.com"
    val adminsToEmail = Set("bobby@example.com", "daisy@example.com")
    val newCollaborator = buildCollaborator(newTeamMemberEmail, Role.ADMINISTRATOR)
    val addCollaboratorRequest = AddCollaboratorToTpaRequest(requestorEmail, newCollaborator, isRegistered = true, adminsToEmail)
    val url = s"$baseUrl/application/${applicationId.value}/collaborator"
  }

  "addCollaborator" should {
    "return success" in new CollaboratorSetup {
      val addCollaboratorResponse = AddCollaboratorToTpaResponse(true)

      when(mockHttpClient.POST[AddCollaboratorToTpaRequest, Either[UpstreamErrorResponse, AddCollaboratorToTpaResponse]](eqTo(url), eqTo(addCollaboratorRequest), *)(*, *, *, *))
      .thenReturn(successful(Right(addCollaboratorResponse)))

      val result: AddCollaboratorResult = await(connector.addCollaborator(applicationId, addCollaboratorRequest))

      result shouldEqual AddCollaboratorSuccessResult(addCollaboratorResponse.registeredUser)
    }

    "return teamMember already exists response" in new CollaboratorSetup {
      when(mockHttpClient.POST[AddCollaboratorToTpaRequest, Either[UpstreamErrorResponse, AddCollaboratorToTpaResponse]](eqTo(url), eqTo(addCollaboratorRequest), *)(*, *, *, *))
      .thenReturn(successful(Left(UpstreamErrorResponse("", CONFLICT))))

      val result: AddCollaboratorResult = await(connector.addCollaborator(applicationId, addCollaboratorRequest))

      result shouldEqual CollaboratorAlreadyExistsFailureResult
    }

    "return application not found response" in new CollaboratorSetup {
      when(mockHttpClient.POST[AddCollaboratorToTpaRequest, Either[UpstreamErrorResponse, AddCollaboratorToTpaResponse]](eqTo(url), eqTo(addCollaboratorRequest), *)(*, *, *, *))
      .thenReturn(successful(Left(UpstreamErrorResponse("", NOT_FOUND))))

      intercept[ApplicationNotFound] {
        await(connector.addCollaborator(applicationId, addCollaboratorRequest))
      }
    }
  }
}
