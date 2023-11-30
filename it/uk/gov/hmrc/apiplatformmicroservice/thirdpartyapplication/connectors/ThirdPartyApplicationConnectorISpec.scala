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

import uk.gov.hmrc.apiplatformmicroservice.common.ProxiedHttpClient
import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.http.HttpClient
import play.api.http.Status._

import scala.concurrent.ExecutionContext.Implicits.global
import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.apiplatformmicroservice.common.utils.WireMockSugarExtensions
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import uk.gov.hmrc.apiplatform.modules.common.domain.models.Environment.{PRODUCTION, SANDBOX}
import uk.gov.hmrc.apiplatformmicroservice.utils.PrincipalAndSubordinateWireMockSetup
import uk.gov.hmrc.apiplatformmicroservice.utils.ConfigBuilder
import uk.gov.hmrc.apiplatformmicroservice.common.builder._
import uk.gov.hmrc.http.UpstreamErrorResponse
import AbstractThirdPartyApplicationConnector._
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.common.domain.models.Environment
import uk.gov.hmrc.apiplatform.modules.common.domain.models.UserId
import uk.gov.hmrc.apiplatformmicroservice.common.utils.UpliftRequestSamples
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.SubscriptionsHelper._
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.Collaborator
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.Collaborators
import uk.gov.hmrc.apiplatform.modules.common.services.ClockNow
import java.time.Clock
import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.Access
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.RedirectUri
import uk.gov.hmrc.apiplatform.modules.applications.core.interface.models.CreateApplicationRequestV1
import uk.gov.hmrc.apiplatform.modules.applications.core.interface.models.CreateApplicationRequestV2
import uk.gov.hmrc.apiplatform.modules.applications.core.interface.models.StandardAccessDataToCopy

class ThirdPartyApplicationConnectorISpec
    extends AsyncHmrcSpec
    with ClockNow
    with WireMockSugarExtensions
    with GuiceOneServerPerSuite
    with ConfigBuilder
    with PrincipalAndSubordinateWireMockSetup
    with ApplicationBuilder {

  override val clock            = Clock.systemUTC()
  private val helloWorldContext = ApiContext("hello-world")
  private val versionOne        = ApiVersionNbr("1.0")
  private val versionTwo        = ApiVersionNbr("2.0")

  private val applicationIdOne = ApplicationId.random
  private val applicationIdTwo = ApplicationId.random

  trait Setup {
    implicit val applicationResponseWrites: Writes[ApplicationIdResponse] = Json.writes[ApplicationIdResponse]

    implicit val hc: HeaderCarrier                     = HeaderCarrier()
    val httpClient                      = app.injector.instanceOf[HttpClient]
    protected val mockProxiedHttpClient = mock[ProxiedHttpClient]
    val apiKeyTest                      = "5bb51bca-8f97-4f2b-aee4-81a4a70a42d3"
    val bearer                          = "TestBearerToken"

    val config                                            = AbstractThirdPartyApplicationConnector.Config(
      applicationBaseUrl = s"http://$WireMockHost:$WireMockPrincipalPort",
      applicationUseProxy = false,
      applicationBearerToken = bearer,
      applicationApiKey = apiKeyTest
    )
    val connector: AbstractThirdPartyApplicationConnector = new PrincipalThirdPartyApplicationConnector(config, httpClient, mockProxiedHttpClient)
  }

  trait SubordinateSetup extends Setup {

    override val config    = AbstractThirdPartyApplicationConnector.Config(
      applicationBaseUrl = s"http://$WireMockHost:$WireMockSubordinatePort",
      applicationUseProxy = false,
      applicationBearerToken = bearer,
      applicationApiKey = apiKeyTest
    )
    override val connector = new SubordinateThirdPartyApplicationConnector(config, httpClient, mockProxiedHttpClient)
  }

  trait ApplicationCreateSetup extends Setup with UpliftRequestSamples {

    private val standardAccess =
      Access.Standard(List(RedirectUri.unsafeApply("https://example.com/redirect")), Some("https://example.com/terms"), Some("https://example.com/privacy"))

    private val collaborators: Set[Collaborator] = Set(
      Collaborators.Administrator(UserId.random, "admin@example.com".toLaxEmail),
      Collaborators.Developer(UserId.random, "dev@example.com".toLaxEmail)
    )

    val createAppRequestV1 = CreateApplicationRequestV1(
      name = "V1 Create Application Request",
      access = standardAccess,
      description = None,
      environment = Environment.PRODUCTION,
      collaborators = collaborators,
      subscriptions = Some(Set(ApiIdentifier.random))
    )

    val createAppRequestV2 = CreateApplicationRequestV2(
      name = "V2 Create Application Request",
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
              .withJsonBody(ApplicationIdResponse(appId))
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
              .withJsonBody(ApplicationIdResponse(appId))
          )
      )
      await(connector.createApplicationV2(createAppRequestV2))
    }
  }

  "fetchApplications for a collaborator by user id" should {
    val userId               = UserId.random
    val url                  = s"/developer/${userId.value}/applications"
    val applicationResponses = List(ApplicationIdResponse(applicationIdOne), ApplicationIdResponse(applicationIdTwo))

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
    val url    = s"/developer/${userId.value}/subscriptions"

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
    val url           = s"/application/${applicationId.value}"
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
    val applicationId = ApplicationId.random
    val url           = s"/application/${applicationId.value}/subscription"

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
}
