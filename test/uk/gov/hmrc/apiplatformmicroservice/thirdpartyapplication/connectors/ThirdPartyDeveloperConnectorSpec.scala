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

import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.Helpers.JSON
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.{ApiContext, ApiIdentifier, ApiVersion}
import uk.gov.hmrc.apiplatformmicroservice.common.ProxiedHttpClient
import uk.gov.hmrc.apiplatformmicroservice.common.builder.CollaboratorsBuilder
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.AbstractThirdPartyApplicationConnector.{ApplicationNotFound, ApplicationResponse, TeamMemberAlreadyExists}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.domain.{AddCollaboratorToTpaRequest, AddCollaboratorToTpaResponse}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.{Application, Role}
import uk.gov.hmrc.apiplatformmicroservice.util.AsyncHmrcSpec
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Future.failed

class ThirdPartyDeveloperConnectorSpec extends AsyncHmrcSpec {

//  private val helloWorldContext = ApiContext("hello-world")
//  private val versionOne = ApiVersion("1.0")
//  private val versionTwo = ApiVersion("2.0")
//
//  private val applicationIdOne = ApplicationId("app id 1")
//  private val applicationIdTwo = ApplicationId("app id 2")

  private val baseUrl = "https://example.com"

  class Setup(proxyEnabled: Boolean = false) {
    implicit val hc = HeaderCarrier()
    protected val mockHttpClient = mock[HttpClient]
    protected val mockProxiedHttpClient = mock[ProxiedHttpClient]
    val apiKeyTest = "5bb51bca-8f97-4f2b-aee4-81a4a70a42d3"
    val bearer = "TestBearerToken"

    val connector = new AbstractThirdPartyDeveloperConnector {
      val httpClient = mockHttpClient
      val proxiedHttpClient = mockProxiedHttpClient

      val config = AbstractThirdPartyDeveloperConnector.Config(
        baseUrl,
        proxyEnabled,
        bearer,
        apiKeyTest
      )
    }
  }

  class SubordinateSetup(proxyEnabled: Boolean = false) extends Setup(proxyEnabled) {

    val config = AbstractThirdPartyDeveloperConnector.Config(
      baseUrl,
      proxyEnabled,
      bearer,
      apiKeyTest
    )

    override val connector = new SubordinateThirdPartyDeveloperConnector(
      config,
      mockHttpClient,
      mockProxiedHttpClient
    ) {}
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

  "addTeamMember" should {
    "return success" in new CollaboratorSetup {
      val addTeamMemberResponse = AddCollaboratorToTpaResponse(true)

      when(
        mockHttpClient
          .POST[AddCollaboratorToTpaRequest, HttpResponse](eqTo(url), eqTo(addCollaboratorRequest), eqTo(Seq(CONTENT_TYPE -> JSON)))(*, *, *, *)
      ).thenReturn(Future.successful(HttpResponse(OK, Some(Json.toJson(addTeamMemberResponse)))))

      val result = await(connector.addCollaborator(applicationId, addCollaboratorRequest))

      result shouldEqual addTeamMemberResponse
    }

    "return teamMember already exists response" in new CollaboratorSetup {
      when(
        mockHttpClient
          .POST[AddCollaboratorToTpaRequest, HttpResponse](eqTo(url), eqTo(addCollaboratorRequest), eqTo(Seq(CONTENT_TYPE -> JSON)))(*, *, *, *)
      ).thenReturn(failed(Upstream4xxResponse("409 exception", CONFLICT, CONFLICT)))

      intercept[TeamMemberAlreadyExists] {
        await(connector.addCollaborator(applicationId, addCollaboratorRequest))
      }
    }

    "return application not found response" in new CollaboratorSetup {
      when(
        mockHttpClient
          .POST[AddCollaboratorToTpaRequest, HttpResponse](eqTo(url), eqTo(addCollaboratorRequest), eqTo(Seq(CONTENT_TYPE -> JSON)))(*, *, *, *)
      ).thenReturn(failed(new NotFoundException("")))

      intercept[ApplicationNotFound] {
        await(connector.addCollaborator(applicationId, addCollaboratorRequest))
      }
    }
  }
}
