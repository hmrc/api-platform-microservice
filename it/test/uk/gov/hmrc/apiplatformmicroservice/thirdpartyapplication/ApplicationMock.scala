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

import java.util.UUID

import com.github.tomakehurst.wiremock.client.WireMock._

import play.api.http.Status._
import play.api.http._

import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, ClientId, Environment, UserId}
import uk.gov.hmrc.apiplatformmicroservice.utils.PrincipalAndSubordinateWireMockSetup

trait ApplicationMock {
  self: PrincipalAndSubordinateWireMockSetup => // To allow for stubFor to work with environment

  def mockFetchApplicationNotFound(env: Environment, applicationId: ApplicationId): Unit = {
    stubFor(env)(get(urlEqualTo(s"/application/$applicationId"))
      .willReturn(
        aResponse()
          .withStatus(NOT_FOUND)
      ))
  }

  def mockFetchApplicationsForDeveloperNotFound(deployedTo: Environment, userId: UserId): Unit = {
    stubFor(deployedTo)(get(urlEqualTo(s"/developer/$userId/applications"))
      .willReturn(aResponse()
        .withStatus(NOT_FOUND)))
  }

  def mockFetchApplicationsForDeveloper(deployedTo: Environment, userId: UserId): Unit = {
    stubFor(deployedTo)(get(urlEqualTo(s"/developer/$userId/applications"))
      .willReturn(
        aResponse()
          .withBody(s"""[{
                       |  "id": "${UUID.randomUUID()}",
                       |  "clientId": "somCLientId",
                       |  "gatewayId": "w7dwd9GFZX",
                       |  "name": "giu",
                       |  "deployedTo": "$deployedTo",
                       |  "description": "Some test data",
                       |  "collaborators": [
                       |      {
                       |          "userId": "${UserId.random}",
                       |          "emailAddress": "bobby.taxation@digital.hmrc.gov.uk",
                       |          "role": "ADMINISTRATOR"
                       |      }
                       |  ],
                       |  "createdOn": 1504526587272,
                       |  "lastAccess": 1561071600000,
                       |  "grantLength": 547,
                       |  "redirectUris": [],
                       |  "access": {
                       |      "accessType": "STANDARD",
                       |      "overrides": [],
                       |      "redirectUris": []
                       |  },
                       |  "state": {
                       |      "name": "PRODUCTION",
                       |      "updatedOn": 1504784641632
                       |  },
                       |  "rateLimitTier": "BRONZE",
                       |  "blocked": false,
                       |  "ipWhitelist": [],
                       |  "ipAllowlist": {
                       |      "required": false,
                       |      "allowlist": []
                       |  },
                       |  "moreApplication": {
                       |      "allowAutoDelete": true
                       |  },
                       |  "trusted": false
                       |}]""".stripMargin)
          .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
          .withStatus(OK)
      ))
  }

  def mockFetchApplication(deployedTo: Environment, applicationId: ApplicationId, clientId: ClientId = ClientId.random): Unit = {
    stubFor(deployedTo)(get(urlEqualTo(s"/application/$applicationId"))
      .willReturn(
        aResponse()
          .withBody(s"""{
                       |  "id": "$applicationId",
                       |  "clientId": "$clientId",
                       |  "gatewayId": "w7dwd9GFZX",
                       |  "name": "giu",
                       |  "deployedTo": "$deployedTo",
                       |  "description": "Some test data",
                       |  "collaborators": [
                       |      {
                       |          "userId": "${UserId.random}",
                       |          "emailAddress": "bobby.taxation@digital.hmrc.gov.uk",
                       |          "role": "ADMINISTRATOR"
                       |      }
                       |  ],
                       |  "createdOn": 1504526587272,
                       |  "lastAccess": 1561071600000,
                       |  "grantLength": 547,
                       |  "redirectUris": [],
                       |  "access": {
                       |      "accessType": "STANDARD",
                       |      "overrides": [],
                       |      "redirectUris": []
                       |  },
                       |  "state": {
                       |      "name": "PRODUCTION",
                       |      "updatedOn": 1504784641632
                       |  },
                       |  "rateLimitTier": "BRONZE",
                       |  "blocked": false,
                       |  "ipWhitelist": [],
                       |  "ipAllowlist": {
                       |      "required": false,
                       |      "allowlist": []
                       |  },
                       |  "moreApplication": {
                       |      "allowAutoDelete": true
                       |  },
                       |  "trusted": false
                       |}""".stripMargin)
          .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
          .withStatus(OK)
      ))
  }

  def mockFetchSubscriptionsForDeveloperNotFound(env: Environment, userId: UserId): Unit = {
    stubFor(env)(get(urlEqualTo(s"/developer/$userId/subscriptions"))
      .willReturn(
        aResponse()
          .withStatus(NOT_FOUND)
      ))
  }

  def mockFetchSubscriptionsForDeveloper(env: Environment, userId: UserId): Unit = {
    stubFor(env)(get(urlEqualTo(s"/developer/$userId/subscriptions"))
      .willReturn(
        aResponse()
          .withBody("""
                      |[
                      |    {
                      |        "context": "individual-benefits",
                      |        "version": "1.0"
                      |    },
                      |    {
                      |        "context": "individual-employment",
                      |        "version": "1.0"
                      |    }
                      |]
          """.stripMargin)
          .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
          .withStatus(OK)
      ))
  }

  def mockFetchApplicationSubscriptions(env: Environment, applicationId: ApplicationId): Unit = {
    stubFor(env)(get(urlEqualTo(s"/application/$applicationId/subscription"))
      .willReturn(
        aResponse()
          .withBody("""
                      |[
                      |    {
                      |        "context": "individual-benefits",
                      |        "version": "1.0"
                      |    },
                      |    {
                      |        "context": "individual-employment",
                      |        "version": "1.0"
                      |    }
                      |]
          """.stripMargin)
          .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
          .withStatus(OK)
      ))
  }
}
