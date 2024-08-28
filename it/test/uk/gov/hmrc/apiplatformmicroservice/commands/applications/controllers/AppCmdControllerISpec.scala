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
    val applicationId              = ApplicationId.random
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

      stubForApplication(applicationId, ClientId.random, UserId.random, UserId.random)

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

  private def stubForApplication(applicationId: ApplicationId, clientId: ClientId, userId1: UserId, userId2: UserId) = {
    stubFor(Environment.SANDBOX)(
      get(urlPathEqualTo(s"/application/$applicationId"))
        .willReturn(
          aResponse()
            .withStatus(NOT_FOUND)
            .withHeader("Content-Type", "application/json")
        )
    )
    stubFor(Environment.PRODUCTION)(
      get(urlPathEqualTo(s"/application/$applicationId"))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withHeader("Content-Type", "application/json")
            .withBody(getBody(applicationId, clientId, userId1, userId2))
        )
    )
  }

  private def getBody(applicationId: ApplicationId, clientId: ClientId, userId1: UserId, userId2: UserId) = {
    s"""{
       |  "id": "$applicationId",
       |  "clientId": "$clientId",
       |  "gatewayId": "gateway-id",
       |  "name": "Petes test application",
       |  "deployedTo": "PRODUCTION",
       |  "description": "Petes test application description",
       |  "collaborators": [
       |    {
       |      "userId": "$userId1",
       |      "emailAddress": "bob@example.com",
       |      "role": "ADMINISTRATOR"
       |    },
       |    {
       |      "userId": "$userId2",
       |      "emailAddress": "bob@example.com",
       |      "role": "ADMINISTRATOR"
       |    }
       |  ],
       |  "createdOn": "$nowAsText",
       |  "lastAccess": "$nowAsText",
       |  "grantLength": 547,
       |  "redirectUris": [],
       |  "access": {
       |    "redirectUris": [],
       |    "overrides": [],
       |    "importantSubmissionData": {
       |      "organisationUrl": "https://www.example.com",
       |      "responsibleIndividual": {
       |        "fullName": "Bob Fleming",
       |        "emailAddress": "bob@example.com"
       |      },
       |      "serverLocations": [
       |        {
       |          "serverLocation": "inUK"
       |        }
       |      ],
       |      "termsAndConditionsLocation": {
       |        "termsAndConditionsType": "inDesktop"
       |      },
       |      "privacyPolicyLocation": {
       |        "privacyPolicyType": "inDesktop"
       |      },
       |      "termsOfUseAcceptances": [
       |        {
       |          "responsibleIndividual": {
       |            "fullName": "Bob Fleming",
       |            "emailAddress": "bob@example.com"
       |          },
       |          "dateTime": "$nowAsText",
       |          "submissionId": "4e62811a-7ab3-4421-a89e-65a8bad9b6ae",
       |          "submissionInstance": 0
       |        }
       |      ]
       |    },
       |    "accessType": "STANDARD"
       |  },
       |  "state": {
       |    "name": "TESTING",
       |    "updatedOn": "$nowAsText"
       |  },
       |  "rateLimitTier": "BRONZE",
       |  "blocked": false,
       |  "trusted": false,
       |  "ipAllowlist": {
       |    "required": false,
       |    "allowlist": []
       |  },
       |  "moreApplication": {
       |    "allowAutoDelete": false,
       |    "lastActionActor": "GATEKEEPER"
       |
       |  }
       |}""".stripMargin
  }
}
