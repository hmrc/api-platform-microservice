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

import scala.concurrent.ExecutionContext

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatestplus.play.guice.GuiceOneServerPerSuite

import play.api.http.Status._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.Environment.{PRODUCTION, SANDBOX}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, Environment, UserId, _}
import uk.gov.hmrc.apiplatform.modules.common.services.ClockNow
import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock
import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.Access
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationName, ApplicationWithCollaboratorsFixtures, Collaborator, Collaborators, LoginRedirectUri}
import uk.gov.hmrc.apiplatform.modules.applications.core.interface.models.{CreateApplicationRequestV1, CreateApplicationRequestV2, StandardAccessDataToCopy}
import uk.gov.hmrc.apiplatformmicroservice.common.utils.{AsyncHmrcSpec, UpliftRequestSamples, WireMockSugarExtensions}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.SubscriptionsHelper._
import uk.gov.hmrc.apiplatformmicroservice.utils.{ConfigBuilder, PrincipalAndSubordinateWireMockSetup}

class ThirdPartyApplicationConnectorISpec
    extends AsyncHmrcSpec
    with ClockNow
    with WireMockSugarExtensions
    with GuiceOneServerPerSuite
    with ConfigBuilder
    with PrincipalAndSubordinateWireMockSetup
    with ApplicationWithCollaboratorsFixtures
    with ApiIdentifierFixtures
    with FixedClock {

  trait Setup {

    implicit val hc: HeaderCarrier    = HeaderCarrier()
    val httpClient                    = app.injector.instanceOf[HttpClientV2]
    implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

    val apiKeyTest = "5bb51bca-8f97-4f2b-aee4-81a4a70a42d3"
    val bearer     = "TestBearerToken"

    val config                                            = AbstractThirdPartyApplicationConnector.Config(
      applicationBaseUrl = s"http://$WireMockHost:$WireMockPrincipalPort",
      applicationUseProxy = false,
      applicationBearerToken = bearer,
      applicationApiKey = apiKeyTest
    )
    val connector: AbstractThirdPartyApplicationConnector = new PrincipalThirdPartyApplicationConnector(config, httpClient)
  }

  trait SubordinateSetup extends Setup {

    override val config    = AbstractThirdPartyApplicationConnector.Config(
      applicationBaseUrl = s"http://$WireMockHost:$WireMockSubordinatePort",
      applicationUseProxy = false,
      applicationBearerToken = bearer,
      applicationApiKey = apiKeyTest
    )
    override val connector = new SubordinateThirdPartyApplicationConnector(config, httpClient)
  }

  trait ApplicationCreateSetup extends Setup with UpliftRequestSamples {

    private val standardAccess =
      Access.Standard(List(LoginRedirectUri.unsafeApply("https://example.com/redirect")), List.empty, Some("https://example.com/terms"), Some("https://example.com/privacy"))

    private val collaborators: Set[Collaborator] = Set(
      Collaborators.Administrator(UserId.random, "admin@example.com".toLaxEmail),
      Collaborators.Developer(UserId.random, "dev@example.com".toLaxEmail)
    )

    val createAppRequestV1 = CreateApplicationRequestV1(
      name = ApplicationName("V1 Create Application Request"),
      access = standardAccess,
      description = None,
      environment = Environment.PRODUCTION,
      collaborators = collaborators,
      subscriptions = Some(Set(ApiIdentifier.random))
    )

    val createAppRequestV2 = CreateApplicationRequestV2(
      name = ApplicationName("V2 Create Application Request"),
      access = StandardAccessDataToCopy(standardAccess.redirectUris, standardAccess.overrides),
      description = None,
      environment = Environment.PRODUCTION,
      collaborators = collaborators,
      upliftRequest = makeUpliftRequest(ApiIdentifier.random),
      "bob@example.com",
      ApplicationId.random
    )
  }

  "create application v1" should {
    val url   = "/application"
    val appId = ApplicationId.random

    "return application Id" in new ApplicationCreateSetup {
      stubFor(PRODUCTION)(
        post(urlEqualTo(url))
          .withJsonRequestBody(createAppRequestV1)
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withJsonBody(standardApp.withId(appId))
          )
      )
      await(connector.createApplicationV1(createAppRequestV1))
    }
  }

  "create application v2" should {
    val url   = "/application"
    val appId = ApplicationId.random

    "return application Id" in new ApplicationCreateSetup {
      stubFor(PRODUCTION)(
        post(urlEqualTo(url))
          .withJsonRequestBody(createAppRequestV2)
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withJsonBody(standardApp.withId(appId))
          )
      )
      await(connector.createApplicationV2(createAppRequestV2))
    }
  }

  "fetchApplications for a collaborator by user id" should {
    val userId               = UserId.random
    val url                  = s"/developer/${userId}/applications"
    val applicationResponses = List(standardApp.withId(applicationIdOne), standardApp.withId(applicationIdTwo))

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
      result should contain.allOf(applicationIdOne, applicationIdTwo)
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

  "fetchSubscriptions for a collaborator by userId" should {
    val userId = UserId.random
    val url    = s"/developer/${userId}/subscriptions"

    val expectedSubscriptions = Seq(apiIdentifierOne, apiIdentifierTwo)

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
    val url = s"/application/${applicationIdOne}"
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
        await(connector.fetchApplication(applicationIdOne))
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
      await(connector.fetchApplication(applicationIdOne)) shouldBe None
    }

    "return the application" in new Setup {
      val application = standardApp.inSandbox()

      stubFor(PRODUCTION)(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withJsonBody(application)
          )
      )

      await(connector.fetchApplication(applicationIdOne)) shouldBe Some(application)
    }
  }

  "fetchSubscriptions" should {
    import AbstractThirdPartyApplicationConnector._
    val url = s"/application/${applicationIdOne}/subscription"

    "propagate error when endpoint returns 5xx error" in new Setup {
      stubFor(PRODUCTION)(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )
      intercept[UpstreamErrorResponse] {
        await(connector.fetchSubscriptionsById(applicationIdOne))
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

      await(connector.fetchSubscriptionsById(applicationIdOne)) shouldBe Set.empty
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
        await(connector.fetchSubscriptionsById(applicationIdOne))
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

      await(connector.fetchSubscriptionsById(applicationIdOne)) shouldBe Set.empty
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

      await(connector.fetchSubscriptionsById(applicationIdOne)) shouldBe Set(
        ApiIdentifier(ContextA, VersionOne),
        ApiIdentifier(ContextB, VersionTwo)
      )
    }
  }
}
