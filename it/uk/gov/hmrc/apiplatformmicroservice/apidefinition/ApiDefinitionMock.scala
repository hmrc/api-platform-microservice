package uk.gov.hmrc.apiplatformmicroservice.apidefinition

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http._
import play.api.http.Status._
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.APICategoryDetails
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.Environment
import uk.gov.hmrc.apiplatformmicroservice.utils.PrincipalAndSubordinateWireMockSetup
import uk.gov.hmrc.apiplatformmicroservice.common.utils.WireMockSugarExtensions
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.APIDefinition
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.controllers.ApiDefinitionController.JsonFormatters._

trait ApiDefinitionMock extends WireMockSugarExtensions {
  self: PrincipalAndSubordinateWireMockSetup => // To allow for stubFor to work with environment

  def whenGetDefinition(env: Environment)(serviceName: String, apiDefinition: APIDefinition) = {
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
  
  def whenGetAllDefinitions(env: Environment)(definitions: APIDefinition*) = {
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

  def whenGetAPICategoryDetails(env: Environment)(categories: APICategoryDetails*): Unit = {
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
                        |      "requiresTrust": true
                        |  }
                        |]""".stripMargin)
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withStatus(OK)
        )
    )
  }

  def mockFetchAPICategoryDetails(environment: Environment, categories: Seq[APICategoryDetails]) {
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

}
