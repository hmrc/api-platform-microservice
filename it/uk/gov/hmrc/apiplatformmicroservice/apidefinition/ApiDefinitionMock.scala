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
import play.api.http._
import play.api.http.Status._
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiCategoryDetails
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.Environment
import uk.gov.hmrc.apiplatformmicroservice.utils.PrincipalAndSubordinateWireMockSetup
import uk.gov.hmrc.apiplatformmicroservice.common.utils.WireMockSugarExtensions
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiDefinition
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.controllers.ApiDefinitionController.JsonFormatters._
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiVersion
import play.api.libs.json.JsValue
import play.api.libs.json.Json

trait ApiDefinitionMock extends WireMockSugarExtensions {
  self: PrincipalAndSubordinateWireMockSetup => // To allow for stubFor to work with environment

  def whenGetDefinition(env: Environment)(serviceName: String, apiDefinition: ApiDefinition) = {
    stubFor(env)(
      get(urlEqualTo(s"/api-definition/$serviceName"))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withJsonBody(apiDefinition)
        )
    )
  }

  def whenGetDefinitionFindsNothing(env: Environment)(serviceName: String) = {
    stubFor(env)(
      get(urlEqualTo(s"/api-definition/$serviceName"))
        .willReturn(
          aResponse()
            .withStatus(NOT_FOUND)
        )
    )    
  }

  def whenGetDefinitionFails(env: Environment)(serviceName: String, statusCode: Int) = {
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

  def whenGetAPICategoryDetails(env: Environment)(categories: ApiCategoryDetails*): Unit = {
    stubFor(env)(
      get(urlPathEqualTo(s"/api-categories"))
      .willReturn(
        aResponse()
        .withStatus(OK)
        .withJsonBody(categories.toList)
      )
    )
  }

  def whenGetAPICategoryDetailsFails(env: Environment)(statusCode: Int): Unit = {
    stubFor(env)(
      get(urlPathEqualTo(s"/api-categories"))
      .willReturn(
        aResponse()
        .withStatus(statusCode)
      )
    )
  }

  def mockFetchApiDefinition(env: Environment) {
    stubFor(env)(
      get(urlEqualTo("/api-definition?type=all"))
        .willReturn(
          aResponse()
            .withBody("""[
                        |  {
                        |      "serviceName": "api-example-microservice",
                        |      "serviceBaseUrl": "http://localhost:9601",
                        |      "name": "Hello World",
                        |      "description": "A 'hello world' example of an API on the HMRC API Developer Hub.",
                        |      "context": "hello",
                        |      "versions": [
                        |          {
                        |              "version": "0.5",
                        |              "status": "RETIRED",
                        |              "endpoints": [
                        |                  {
                        |                      "uriPattern": "/world",
                        |                      "endpointName": "Say hello world",
                        |                      "method": "GET",
                        |                      "authType": "USER",
                        |                      "throttlingTier": "UNLIMITED"
                        |                  }
                        |              ],
                        |              "endpointsEnabled": true
                        |          },
                        |          {
                        |              "version": "5.0",
                        |              "status": "ALPHA",
                        |              "endpoints": [
                        |                  {
                        |                      "uriPattern": "/world",
                        |                      "endpointName": "Say hello world",
                        |                      "method": "GET",
                        |                      "authType": "NONE",
                        |                      "throttlingTier": "UNLIMITED"
                        |                  }
                        |              ],
                        |              "endpointsEnabled": false
                        |          },
                        |          {
                        |              "version": "3.0",
                        |              "status": "STABLE",
                        |              "endpoints": [
                        |                  {
                        |                      "uriPattern": "/world",
                        |                      "endpointName": "Say hello world",
                        |                      "method": "GET",
                        |                      "authType": "NONE",
                        |                      "throttlingTier": "UNLIMITED"
                        |                  }
                        |              ],
                        |              "endpointsEnabled": true
                        |          },
                        |          {
                        |              "version": "2.5rc",
                        |              "status": "STABLE",
                        |              "endpoints": [
                        |                  {
                        |                      "uriPattern": "/world",
                        |                      "endpointName": "Say hello world",
                        |                      "method": "GET",
                        |                      "authType": "NONE",
                        |                      "throttlingTier": "UNLIMITED"
                        |                  }
                        |              ],
                        |              "endpointsEnabled": true
                        |          },
                        |          {
                        |              "version": "1.0",
                        |              "status": "STABLE",
                        |              "endpoints": [
                        |                  {
                        |                      "uriPattern": "/world",
                        |                      "endpointName": "Say hello world",
                        |                      "method": "GET",
                        |                      "authType": "NONE",
                        |                      "throttlingTier": "UNLIMITED"
                        |                  }
                        |              ],
                        |              "endpointsEnabled": true
                        |          },
                        |          {
                        |              "version": "2.0",
                        |              "status": "STABLE",
                        |              "endpoints": [
                        |                  {
                        |                      "uriPattern": "/world2",
                        |                      "endpointName": "Say hello world",
                        |                      "method": "GET",
                        |                      "authType": "NONE",
                        |                      "throttlingTier": "UNLIMITED"
                        |                  }
                        |              ],
                        |              "endpointsEnabled": true
                        |          },
                        |          {
                        |              "version": "4.0",
                        |              "status": "DEPRECATED",
                        |              "endpoints": [
                        |                  {
                        |                      "uriPattern": "/world2",
                        |                      "endpointName": "Say hello world",
                        |                      "method": "GET",
                        |                      "authType": "USER",
                        |                      "throttlingTier": "UNLIMITED"
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
                        |      "versions": [
                        |          {
                        |              "version": "1.0",
                        |              "status": "STABLE",
                        |              "endpoints": [
                        |                  {
                        |                      "uriPattern": "/world",
                        |                      "endpointName": "Say hello world",
                        |                      "method": "GET",
                        |                      "authType": "NONE",
                        |                      "throttlingTier": "UNLIMITED"
                        |                  }
                        |              ],
                        |              "endpointsEnabled": true
                        |          }
                        |      ],
                        |      "lastPublishedAt": "2018-07-13T13:18:06.124Z"
                        |  },
                        |  {
                        |      "serviceName": "api-example-trusted",
                        |      "serviceBaseUrl": "http://localhost:9603",
                        |      "name": "Hello Trust",
                        |      "description": "A 'hello another' example of an API on the HMRC API Developer Hub.",
                        |      "context": "trusted",
                        |      "versions": [
                        |          {
                        |              "version": "1.0",
                        |              "status": "STABLE",
                        |              "endpoints": [
                        |                  {
                        |                      "uriPattern": "/world",
                        |                      "endpointName": "Say hello world",
                        |                      "method": "GET",
                        |                      "authType": "NONE",
                        |                      "throttlingTier": "UNLIMITED"
                        |                  }
                        |              ],
                        |              "endpointsEnabled": true
                        |          }
                        |      ],
                        |      "lastPublishedAt": "2018-07-13T13:18:06.124Z",
                        |      "requiresTrust": true,
                        |      "categories": [
                        |          "EXAMPLE"
                        |      ]
                        |  }
                        |]""".stripMargin)
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withStatus(OK)
        )
    )
  }

  def mockFetchApiCategoryDetails(environment: Environment, categories: Seq[ApiCategoryDetails]) {
    val categoriesJsonString: String =
      categories
        .map(category => s"""{ "category" : "${category.category}", "name" : "${category.name}" }""")
        .mkString("[", ",", "]")

    stubFor(environment)(
      get(urlEqualTo("/api-categories"))
        .willReturn(
          aResponse()
            .withBody(categoriesJsonString)))
  }

  def whenFetchApiSpecification(environment: Environment)(serviceName: String, version: ApiVersion, jsValue: JsValue) = {
    stubFor(environment)(
      get(urlEqualTo(s"/api-definition/$serviceName/${version.value}/specification"))
      .willReturn(
        aResponse()
        .withBody(Json.stringify(jsValue))
      )
    )
  }

  def whenFetchApiSpecificationFindsNothing(environment: Environment)(serviceName: String, version: ApiVersion) = {
    stubFor(environment)(
      get(urlEqualTo(s"/api-definition/$serviceName/${version.value}/specification"))
      .willReturn(
        aResponse()
        .withStatus(NOT_FOUND)
      )
    )
  }
}
