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

package uk.gov.hmrc.apiplatformmicroservice.commands.applications.connectors

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global

import cats.data.NonEmptyList
import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatestplus.play.guice.GuiceOneServerPerSuite

import play.api.http.Status._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, InternalServerException}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock
import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.Access
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models._
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.commands.applications.domain.models.DispatchSuccessResult
import uk.gov.hmrc.apiplatformmicroservice.common.ProxiedHttpClient
import uk.gov.hmrc.apiplatformmicroservice.common.builder._
import uk.gov.hmrc.apiplatformmicroservice.common.utils.{AsyncHmrcSpec, WireMockSugarExtensions}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications._
import uk.gov.hmrc.apiplatformmicroservice.utils.{ConfigBuilder, PrincipalAndSubordinateWireMockSetup}

class AppCmdConnectorISpec
    extends AsyncHmrcSpec
    with WireMockSugarExtensions
    with GuiceOneServerPerSuite
    with ConfigBuilder
    with PrincipalAndSubordinateWireMockSetup
    with ApplicationBuilder
    with FixedClock {

  trait Setup {

    implicit val hc: HeaderCarrier      = HeaderCarrier()
    val httpClient                      = app.injector.instanceOf[HttpClient]
    protected val mockProxiedHttpClient = mock[ProxiedHttpClient]
    val apiKeyTest                      = "5bb51bca-8f97-4f2b-aee4-81a4a70a42d3"
    val bearer                          = "TestBearerToken"

    val applicationId = ApplicationId.random
    val clientId      = ClientId.random

    def anApplicationResponse(createdOn: Instant = instant, lastAccess: Instant = instant): Application = {
      Application(
        applicationId,
        clientId,
        "gatewayId",
        "appName",
        Environment.PRODUCTION,
        Some("random description"),
        Set.empty,
        instant,
        None,
        GrantLength.EIGHTEEN_MONTHS,
        None,
        Access.Standard(),
        ApplicationState(State.TESTING, None, None, None, updatedOn = instant),
        RateLimitTier.BRONZE,
        None,
        false,
        IpAllowlist(),
        MoreApplication(true)
      )
    }

    val config = AppCmdConnector.Config(
      baseUrl = s"http://$WireMockHost:$WireMockPrincipalPort"
    )

    val connector: AppCmdConnector = new AppCmdConnector(config, httpClient)
    val url                        = s"${config.baseUrl}/application/${applicationId.value}/dispatch"
  }

  trait CollaboratorSetup extends Setup with CollaboratorsBuilder {
    val requestorEmail     = "requestor@example.com".toLaxEmail
    val newTeamMemberEmail = "newTeamMember@example.com".toLaxEmail
    val adminsToEmail      = Set("bobby@example.com".toLaxEmail, "daisy@example.com".toLaxEmail)

    val newCollaborator = Collaborators.Administrator(UserId.random, newTeamMemberEmail)
    val cmd             = ApplicationCommands.AddCollaborator(Actors.AppCollaborator(requestorEmail), newCollaborator, instant)
    val request         = DispatchRequest(cmd, adminsToEmail)
  }

  "addCollaborator" should {
    "return success" in new CollaboratorSetup {
      val response = anApplicationResponse()

      stubFor(Environment.PRODUCTION)(
        patch(urlMatching(s".*/applications/${applicationId.value}/dispatch"))
          .withJsonRequestBody(request)
          .willReturn(
            aResponse()
              .withJsonBody(DispatchSuccessResult(response))
              .withStatus(OK)
          )
      )

      val result = await(connector.dispatch(applicationId, request))

      result.value shouldBe DispatchSuccessResult(response)
    }

    "return teamMember already exists response" in new CollaboratorSetup {
      import uk.gov.hmrc.apiplatform.modules.common.domain.services.NonEmptyListFormatters._
      val response = NonEmptyList.one[CommandFailure](CommandFailures.CollaboratorAlreadyExistsOnApp)

      stubFor(Environment.PRODUCTION)(
        patch(urlMatching(s".*/applications/${applicationId.value}/dispatch"))
          .withJsonRequestBody(request)
          .willReturn(
            aResponse()
              .withJsonBody(response)
              .withStatus(BAD_REQUEST)
          )
      )

      val result = await(connector.dispatch(applicationId, request))

      result.left.value shouldBe NonEmptyList.one(CommandFailures.CollaboratorAlreadyExistsOnApp)
    }

    "return for generic error" in new CollaboratorSetup {

      stubFor(Environment.PRODUCTION)(
        patch(urlMatching(s".*/applications/${applicationId.value}/dispatch"))
          .withJsonRequestBody(request)
          .willReturn(
            aResponse()
              .withStatus(IM_A_TEAPOT)
          )
      )

      intercept[InternalServerException] {
        await(connector.dispatch(applicationId, request))
      }.message shouldBe (s"Failed calling dispatch 418")
    }
  }
}
