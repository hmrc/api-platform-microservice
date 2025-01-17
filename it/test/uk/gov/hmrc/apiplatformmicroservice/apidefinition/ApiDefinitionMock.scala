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

package uk.gov.hmrc.apiplatformmicroservice.apidefinition

import com.github.tomakehurst.wiremock.client.WireMock._

import play.api.http.Status._
import play.api.http._
import play.api.libs.json.{JsValue, Json}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApiVersionNbr, Environment}
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.{ApiDefinition, ServiceName}
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.DisplayApiEvent
import uk.gov.hmrc.apiplatformmicroservice.common.utils.WireMockSugarExtensions
import uk.gov.hmrc.apiplatformmicroservice.utils.PrincipalAndSubordinateWireMockSetup

trait ApiDefinitionMock extends WireMockSugarExtensions {
  self: PrincipalAndSubordinateWireMockSetup => // To allow for stubFor to work with environment

  def whenGetDefinition(env: Environment)(serviceName: ServiceName, apiDefinition: ApiDefinition) = {
    stubFor(env)(
      get(urlEqualTo(s"/api-definition/$serviceName"))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withJsonBody(apiDefinition)
        )
    )
  }

  def whenGetDefinitionFindsNothing(env: Environment)(serviceName: ServiceName) = {
    stubFor(env)(
      get(urlEqualTo(s"/api-definition/$serviceName"))
        .willReturn(
          aResponse()
            .withStatus(NOT_FOUND)
        )
    )
  }

  def whenGetDefinitionFails(env: Environment)(serviceName: ServiceName, statusCode: Int) = {
    stubFor(env)(
      get(urlEqualTo(s"/api-definition/$serviceName"))
        .willReturn(
          aResponse()
            .withStatus(statusCode)
        )
    )
  }

  def whenGetAllDefinitions(env: Environment)(definitions: ApiDefinition*) = {
    stubFor(env)(
      get(urlPathEqualTo(s"/api-definition"))
        .withQueryParam("type", equalTo("all"))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withJsonBody(definitions.toList)
        )
    )
  }

  def whenGetAllDefinitionsFindsNothing(env: Environment): Unit = {
    stubFor(env)(
      get(urlPathEqualTo(s"/api-definition"))
        .withQueryParam("type", equalTo("all"))
        .willReturn(
          aResponse()
            .withStatus(NOT_FOUND)
        )
    )
  }

  def whenGetAllDefinitionsFails(env: Environment)(statusCode: Int): Unit = {
    stubFor(env)(
      get(urlPathEqualTo(s"/api-definition"))
        .withQueryParam("type", equalTo("all"))
        .willReturn(
          aResponse()
            .withStatus(statusCode)
        )
    )
  }

  def mockFetchApiDefinition(env: Environment, anotherApiAuthType: String = "NONE"): Unit = {
    stubFor(env)(
      get(urlEqualTo("/api-definition?type=all"))
        .willReturn(
          aResponse()
            .withBody(s"""[
                         |  {
                         |      "serviceName": "api-example-microservice",
                         |      "serviceBaseUrl": "http://localhost:9601",
                         |      "name": "Hello World",
                         |      "description": "A 'hello world' example of an API on the HMRC API Developer Hub.",
                         |      "context": "hello",
                         |      "isTestSupport": false,
                         |      "categories": [ "OTHER" ],
                         |      "versions": [
                         |          {
                         |              "version": "0.5",
                         |              "status": "RETIRED",
                         |              "access": { "type": "PUBLIC"},
                         |              "versionSource": "UNKNOWN",
                         |              "endpoints": [
                         |                  {
                         |                      "uriPattern": "/world",
                         |                      "endpointName": "Say hello world",
                         |                      "method": "GET",
                         |                      "authType": "USER",
                         |                      "throttlingTier": "UNLIMITED",
                         |                      "queryParameters": []
                         |                  }
                         |              ],
                         |              "endpointsEnabled": true
                         |          },
                         |          {
                         |              "version": "5.0",
                         |              "status": "ALPHA",
                         |              "access": { "type": "PUBLIC"},
                         |              "versionSource": "UNKNOWN",
                         |              "endpoints": [
                         |                  {
                         |                      "uriPattern": "/world",
                         |                      "endpointName": "Say hello world",
                         |                      "method": "GET",
                         |                      "authType": "NONE",
                         |                      "throttlingTier": "UNLIMITED",
                         |                      "queryParameters": []
                         |                  }
                         |              ],
                         |              "endpointsEnabled": false
                         |          },
                         |          {
                         |              "version": "3.0",
                         |              "status": "STABLE",
                         |              "access": { "type": "PUBLIC"},
                         |              "versionSource": "UNKNOWN",
                         |              "endpoints": [
                         |                  {
                         |                      "uriPattern": "/world",
                         |                      "endpointName": "Say hello world",
                         |                      "method": "GET",
                         |                      "authType": "NONE",
                         |                      "throttlingTier": "UNLIMITED",
                         |                      "queryParameters": []  
                         |                  }
                         |              ],
                         |              "endpointsEnabled": true
                         |          },
                         |          {
                         |              "version": "2.5rc",
                         |              "status": "STABLE",
                         |              "access": { "type": "PUBLIC"},
                         |              "versionSource": "UNKNOWN",
                         |              "endpoints": [
                         |                  {
                         |                      "uriPattern": "/world",
                         |                      "endpointName": "Say hello world",
                         |                      "method": "GET",
                         |                      "authType": "NONE",
                         |                      "throttlingTier": "UNLIMITED",
                         |                      "queryParameters": []  
                         |                  }
                         |              ],
                         |              "endpointsEnabled": true
                         |          },
                         |          {
                         |              "version": "1.0",
                         |              "status": "STABLE",
                         |              "access": { "type": "PUBLIC"},
                         |              "versionSource": "UNKNOWN",
                         |              "endpoints": [
                         |                  {
                         |                      "uriPattern": "/world",
                         |                      "endpointName": "Say hello world",
                         |                      "method": "GET",
                         |                      "authType": "NONE",
                         |                      "throttlingTier": "UNLIMITED",
                         |                      "queryParameters": []  
                         |                  }
                         |              ],
                         |              "endpointsEnabled": true
                         |          },
                         |          {
                         |              "version": "2.0",
                         |              "status": "STABLE",
                         |              "access": { "type": "PUBLIC"},
                         |              "versionSource": "UNKNOWN",
                         |              "endpoints": [
                         |                  {
                         |                      "uriPattern": "/world2",
                         |                      "endpointName": "Say hello world",
                         |                      "method": "GET",
                         |                      "authType": "NONE",
                         |                      "throttlingTier": "UNLIMITED",
                         |                      "queryParameters": []  
                         |                  }
                         |              ],
                         |              "endpointsEnabled": true
                         |          },
                         |          {
                         |              "version": "4.0",
                         |              "status": "DEPRECATED",
                         |              "access": { "type": "PUBLIC"},
                         |              "versionSource": "UNKNOWN",
                         |              "endpoints": [
                         |                  {
                         |                      "uriPattern": "/world2",
                         |                      "endpointName": "Say hello world",
                         |                      "method": "GET",
                         |                      "authType": "USER",
                         |                      "throttlingTier": "UNLIMITED",
                         |                      "queryParameters": []  
                         |                  }
                         |              ],
                         |              "endpointsEnabled": true
                         |          }
                         |      ],
                         |      "lastPublishedAt": "2018-07-13T13:18:06.124Z"
                         |  },
                         |  {
                         |      "serviceName": "api-example-another",
                         |      "serviceBaseUrl": "http://localhost:9602",
                         |      "name": "Hello Another",
                         |      "description": "A 'hello another' example of an API on the HMRC API Developer Hub.",
                         |      "context": "another",
                         |      "isTestSupport": false,
                         |      "categories": ["OTHER"],
                         |      "versions": [
                         |          {
                         |              "version": "1.0",
                         |              "status": "STABLE",
                         |              "access": { "type": "PUBLIC"},
                         |              "versionSource": "UNKNOWN",
                         |              "endpoints": [
                         |                  {
                         |                      "uriPattern": "/world",
                         |                      "endpointName": "Say hello world",
                         |                      "method": "GET",
                         |                      "authType": "$anotherApiAuthType",
                         |                      "throttlingTier": "UNLIMITED",
                         |                      "queryParameters": []  
                         |                  }
                         |              ],
                         |              "endpointsEnabled": true
                         |          }
                         |      ],
                         |      "lastPublishedAt": "2018-07-13T13:18:06.124Z"
                         |  }
                         |]""".stripMargin)
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withStatus(OK)
        )
    )
  }

  def whenFetchApiSpecification(environment: Environment)(serviceName: ServiceName, version: ApiVersionNbr, jsValue: JsValue) = {
    stubFor(environment)(
      get(urlEqualTo(s"/api-definition/$serviceName/${version.value}/specification"))
        .willReturn(
          aResponse()
            .withBody(Json.stringify(jsValue))
        )
    )
  }

  def whenFetchApiSpecificationFindsNothing(environment: Environment)(serviceName: ServiceName, version: ApiVersionNbr) = {
    stubFor(environment)(
      get(urlEqualTo(s"/api-definition/$serviceName/${version.value}/specification"))
        .willReturn(
          aResponse()
            .withStatus(NOT_FOUND)
        )
    )
  }

  def whenGetApiEvents(env: Environment)(serviceName: ServiceName, displayApiEvents: List[DisplayApiEvent], includeNoChange: Boolean = true) = {
    stubFor(env)(
      get(urlEqualTo(s"/api-definition/$serviceName/events?includeNoChange=$includeNoChange"))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withJsonBody(displayApiEvents)
        )
    )
  }

  def whenGetApiEventsFails(env: Environment)(serviceName: ServiceName, statusCode: Int) = {
    stubFor(env)(
      get(urlEqualTo(s"/api-definition/$serviceName/events"))
        .willReturn(
          aResponse()
            .withStatus(statusCode)
        )
    )
  }

  def whenGetApiEventsFindsNothing(env: Environment)(serviceName: ServiceName) = {
    stubFor(env)(
      get(urlEqualTo(s"/api-definition/$serviceName/events?includeNoChange=true"))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withBody("[]")
        )
    )
  }

}
