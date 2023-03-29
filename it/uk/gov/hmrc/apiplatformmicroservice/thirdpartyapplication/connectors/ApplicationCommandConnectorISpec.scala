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
import uk.gov.hmrc.apiplatformmicroservice.utils.PrincipalAndSubordinateWireMockSetup
import uk.gov.hmrc.apiplatformmicroservice.utils.ConfigBuilder
import uk.gov.hmrc.apiplatformmicroservice.common.builder._
import AbstractThirdPartyApplicationConnector._
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.ApplicationId
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.Environment
import uk.gov.hmrc.apiplatform.modules.developers.domain.models.UserId
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications._
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax

import java.time.LocalDateTime
import uk.gov.hmrc.apiplatform.modules.common.domain.models.Actors
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.DispatchRequest
import org.joda.time.DateTime
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.ClientId
import java.time.Period
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.Collaborators
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.DispatchSuccessResult
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.CommandFailures
import cats.data.NonEmptyList
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.CommandFailure
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.ApplicationCommands

class ApplicationCommandConnectorISpec
    extends AsyncHmrcSpec
    with WireMockSugarExtensions
    with GuiceOneServerPerSuite
    with ConfigBuilder
    with PrincipalAndSubordinateWireMockSetup
    with ApplicationBuilder {

  trait Setup {
    implicit val applicationResponseWrites = Json.writes[ApplicationResponse]

    implicit val hc                     = HeaderCarrier()
    val httpClient                      = app.injector.instanceOf[HttpClient]
    protected val mockProxiedHttpClient = mock[ProxiedHttpClient]
    val apiKeyTest                      = "5bb51bca-8f97-4f2b-aee4-81a4a70a42d3"
    val bearer                          = "TestBearerToken"

    val applicationId          = ApplicationId.random
    val clientId = ClientId.random

    def anApplicationResponse(createdOn: DateTime = DateTime.now(), lastAccess: DateTime = DateTime.now()): Application = {
      Application(
        applicationId,
        clientId,
        "gatewayId",
        "appName",
        DateTime.now(),
        None,
        Period.ofDays(547),
        None,
        Environment.PRODUCTION,
        Some("random description"),
        Set.empty
      )
    }
  }

  trait PrincipalSetup extends Setup {
    self: Setup =>

    val config = PrincipalApplicationCommandConnector.Config(
      baseUrl = s"http://$WireMockHost:$WireMockPrincipalPort"
    )
      
    val connector: ApplicationCommandConnector = new PrincipalApplicationCommandConnector(config, httpClient)
    val url = s"${config.baseUrl}/application/${applicationId.value}/dispatch"
    println("URL "+url)
  }

  trait SubordinateSetup {
    self: Setup =>

    val config    = SubordinateApplicationCommandConnector.Config(
      baseUrl = s"http://$WireMockHost:$WireMockSubordinatePort",
      useProxy = false,
      bearerToken = bearer,
      apiKey = apiKeyTest
    )
    val connector = new SubordinateApplicationCommandConnector(config, httpClient, mockProxiedHttpClient)
    val url = s"${config.baseUrl}/application/${applicationId.value}/dispatch"
    println("URL "+url)
    }

  trait CollaboratorSetup extends Setup with CollaboratorsBuilder {
    val requestorEmail         = "requestor@example.com".toLaxEmail
    val newTeamMemberEmail     = "newTeamMember@example.com".toLaxEmail
    val adminsToEmail          = Set("bobby@example.com".toLaxEmail, "daisy@example.com".toLaxEmail)

    val newCollaborator        = Collaborators.Administrator(UserId.random, newTeamMemberEmail)
    val cmd = ApplicationCommands.AddCollaborator(Actors.AppCollaborator(requestorEmail), newCollaborator, LocalDateTime.now())
    val request = DispatchRequest(cmd, adminsToEmail)
  }

  "addCollaborator" should {
    "return success" in new CollaboratorSetup with PrincipalSetup {
      val response = anApplicationResponse()

      stubFor(Environment.PRODUCTION)(
        patch(urlMatching(s".*/application/${applicationId.value}/dispatch"))
          .withJsonRequestBody(request)
          .willReturn(
            aResponse()
              .withJsonBody(DispatchSuccessResult(response))
              .withStatus(OK)
          )
      )

      val result = await(connector.dispatch(applicationId, request))
      
      result.right.value shouldBe DispatchSuccessResult(response)
    }

    "return teamMember already exists response" in new CollaboratorSetup with PrincipalSetup {
      import uk.gov.hmrc.apiplatform.modules.common.domain.services.NonEmptyListFormatters._
      val response = NonEmptyList.one[CommandFailure](CommandFailures.CollaboratorAlreadyExistsOnApp)
      
      stubFor(Environment.PRODUCTION)(
        patch(urlMatching(s".*/application/${applicationId.value}/dispatch"))
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

    "return for generic error" in new CollaboratorSetup  with PrincipalSetup {
      
      stubFor(Environment.PRODUCTION)(
        patch(urlMatching(s".*/application/${applicationId.value}/dispatch"))
          .withJsonRequestBody(request)
          .willReturn(
            aResponse()
              .withStatus(IM_A_TEAPOT)
          )
      )

      intercept[InternalServerException] {
        await(connector.dispatch(applicationId, request))
      }.message shouldBe(s"Failed calling dispatch 418")
    }
  }
}
