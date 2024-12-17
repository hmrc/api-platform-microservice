/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.apiplatformmicroservice.commands.applications.controllers

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatestplus.play.guice.GuiceOneServerPerSuite

import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.{Application, Configuration, Mode}
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.utils
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models._
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.apiplatformmicroservice.utils._

class AppCmdControllerISpec
    extends AsyncHmrcSpec
    with GuiceOneServerPerSuite
    with ConfigBuilder
    with PrincipalAndSubordinateWireMockSetup
    with ApplicationWithCollaboratorsFixtures
    with utils.FixedClock {

  private val stubConfig = Configuration(
    "microservice.services.third-party-application-principal.host"   -> WireMockHost,
    "microservice.services.third-party-application-principal.port"   -> WireMockPrincipalPort,
    "microservice.services.third-party-application-subordinate.host" -> WireMockHost,
    "microservice.services.third-party-application-subordinate.port" -> WireMockSubordinatePort,
    "microservice.services.third-party-orchestrator.host"            -> WireMockHost,
    "microservice.services.third-party-orchestrator.port"            -> WireMockPrincipalPort
  )

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(stubConfig)
      .in(Mode.Test)
      .build()

  trait Setup {
    val applicationId              = standardApp.id
    implicit val hc: HeaderCarrier = HeaderCarrier()
    lazy val baseUrl               = s"http://localhost:$port"

    val wsClient           = app.injector.instanceOf[WSClient]
    val requestorEmail     = "requestor@example.com".toLaxEmail
    val newTeamMemberEmail = "newTeamMember@example.com".toLaxEmail
    val newCollaborator    = Collaborators.Administrator(UserId.random, newTeamMemberEmail)
    val cmd                = ApplicationCommands.AddCollaborator(Actors.AppCollaborator(requestorEmail), newCollaborator, instant)
    val request            = DispatchRequest(cmd, Set.empty[LaxEmailAddress])
  }

  "AppCmdController" should {
    "return 401 when Unauthorised is returned from connector" in new Setup {

      stubFor(Environment.PRODUCTION)(
        get(urlPathEqualTo(s"/application/$applicationId"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withHeader("Content-Type", "application/json")
              .withBody(Json.toJson(standardApp).toString())
          )
      )

      stubFor(Environment.PRODUCTION)(
        patch(urlMatching(s"/applications/${applicationId.value}/dispatch"))
          .willReturn(
            aResponse()
              .withStatus(UNAUTHORIZED)
          )
      )
      val body                 = Json.toJson(request).toString()
      val response: WSResponse = await(wsClient.url(s"${baseUrl}/applications/${applicationId.value}/dispatch").withHttpHeaders(("content-type", "application/json")).patch(body))
      response.status shouldBe UNAUTHORIZED
    }
  }

}
