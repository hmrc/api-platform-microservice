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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication

import com.github.tomakehurst.wiremock.client.WireMock._

import play.api.http.Status._
import play.api.http._
import play.api.libs.json.Json

import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationWithCollaboratorsFixtures, Collaborators}
import uk.gov.hmrc.apiplatform.modules.applications.core.interface.models.QueriedApplication
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiDefinitionTestDataHelper
import uk.gov.hmrc.apiplatformmicroservice.common.utils.WireMockSugarExtensions
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.SubscriptionFieldsData
import uk.gov.hmrc.apiplatformmicroservice.utils.PrincipalAndSubordinateWireMockSetup

trait ApplicationMock extends ApplicationWithCollaboratorsFixtures with ApiIdentifierFixtures with WireMockSugarExtensions with ApiDefinitionTestDataHelper
    with SubscriptionFieldsData {
  self: PrincipalAndSubordinateWireMockSetup => // To allow for stubFor to work with environment

  def mockFetchApplicationNotFound(env: Environment, applicationId: ApplicationId): Unit = {
    stubForProd {
      get(urlPathEqualTo(s"/environment/$env/query"))
        .withQueryParam("applicationId", equalTo(applicationId.toString()))
        .willReturn(
          aResponse()
            .withStatus(NOT_FOUND)
        )
    }
  }

  def mockFetchApplication(deployedTo: Environment, applicationId: ApplicationId, clientId: ClientId = ClientId.random): Unit = {
    stubForProd(
      get(urlPathEqualTo(s"/environment/$deployedTo/query"))
        .withQueryParam("applicationId", equalTo(applicationId.toString()))
        .willReturn(
          aResponse()
            .withBody(Json.toJson(
              standardApp.withEnvironment(deployedTo).withId(applicationId)
                .modify(_.copy(token = standardApp.details.token.copy(clientId = clientId)))
            ).toString())
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withStatus(OK)
        )
    )
  }

  def mockFetchApplicationWithFieldsNotFound(env: Environment, applicationId: ApplicationId): Unit = {
    stubForProd {
      get(urlPathEqualTo(s"/environment/$env/query"))
        .withQueryParam("applicationId", equalTo(applicationId.toString()))
        .withQueryParam("wantSubscriptions", equalTo(""))
        .withQueryParam("wantSubscriptionFields", equalTo(""))
        .willReturn(
          aResponse()
            .withStatus(NOT_FOUND)
        )
    }
  }

  def mockFetchApplicationWithFields(deployedTo: Environment, applicationId: ApplicationId, clientId: ClientId = ClientId.random): Unit = {
    stubForProd {
      get(urlPathEqualTo(s"/environment/$deployedTo/query"))
        .withQueryParam("applicationId", equalTo(applicationId.toString()))
        .withQueryParam("wantSubscriptions", equalTo(""))
        .withQueryParam("wantSubscriptionFields", equalTo(""))
        .willReturn(
          aResponse()
            .withBody(Json.toJson(
              QueriedApplication(
                standardApp
                  .withEnvironment(deployedTo)
                  .withId(applicationId)
                  .modify(_.copy(token = standardApp.details.token.copy(clientId = clientId)))
                  .withSubscriptions(Set(
                    context1.asIdentifier(version1),
                    context1.asIdentifier(version2),
                    context2.asIdentifier(version1),
                    context2.asIdentifier(version2),
                    context3.asIdentifier(version1)
                  ))
                  .withFieldValues(fieldValues)
              )
            ).toString())
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withStatus(OK)
        )
    }
  }

  def mockFetchApplicationsForDeveloperNotFound(deployedTo: Environment, userId: UserId): Unit = {
    stubForProd {
      get(urlPathEqualTo(s"/environment/$deployedTo/query"))
        .withQueryParam("userId", equalTo(userId.toString()))
        .willReturn(aResponse()
          .withBody("[]")
          .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
          .withStatus(OK))
    }
  }

  def mockFetchApplicationsForDeveloper(deployedTo: Environment, userId: UserId): Unit = {
    stubForProd {
      get(urlPathEqualTo(s"/environment/$deployedTo/query"))
        .withQueryParam("userId", equalTo(userId.toString()))
        .willReturn(
          aResponse()
            .withBody(Json.toJson(
              List(
                standardApp
                  .withEnvironment(deployedTo)
                  .withCollaborators(Collaborators.Administrator(userId, LaxEmailAddress("bobby.taxation@digital.hmrc.gov.uk")))
              )
            ).toString())
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withStatus(OK)
        )
    }
  }

  def mockFetchSubscriptionsForDeveloperNotFound(env: Environment, userId: UserId): Unit = {
    stubForProd {
      get(urlPathEqualTo(s"/environment/$env/query"))
        .withQueryParam("userId", equalTo(userId.toString()))
        .willReturn(
          aResponse()
            .withBody("[]")
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withStatus(OK)
        )
    }
  }

  def mockFetchSubscriptionsForDeveloper(env: Environment, userId: UserId): Unit = {
    stubForProd {
      get(urlPathEqualTo(s"/environment/$env/query"))
        .withQueryParam("userId", equalTo(userId.toString()))
        .withQueryParam("wantSubscriptions", equalTo(""))
        .willReturn(
          aResponse()
            .withJsonBody(
              List(
                standardApp
                  .withSubscriptions(Set(
                    "individual-benefits".asIdentifier("1.0".asVersion()),
                    "individual-employment".asIdentifier("1.0".asVersion())
                  ))
              )
            )
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withStatus(OK)
        )
    }
  }

  def mockFetchApplicationSubscriptions(env: Environment, applicationId: ApplicationId): Unit = {
    stubForProd {
      get(urlPathEqualTo(s"/environment/$env/query"))
        .withQueryParam("application", equalTo(applicationId.toString()))
        .willReturn(
          aResponse()
            .withJsonBody(
              standardApp.withSubscriptions(Set(
                "individual-benefits".asIdentifier("1.0".asVersion()),
                "individual-employment".asIdentifier("1.0".asVersion())
              ))
            )
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withStatus(OK)
        )
    }
  }
}
