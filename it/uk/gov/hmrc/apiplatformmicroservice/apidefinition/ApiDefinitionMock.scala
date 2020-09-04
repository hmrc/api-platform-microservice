package uk.gov.hmrc.apiplatformmicroservice.apidefinition

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http._
import play.api.http.Status._
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.Environment
import uk.gov.hmrc.apiplatformmicroservice.utils.WiremockSetup

trait ApiDefinitionMock {
  self: WiremockSetup => // To allow for stubFor to work with environment

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
                        |                      "authType": "NONE",
                        |                      "throttlingTier": "UNLIMITED"
                        |                  }
                        |              ],
                        |              "endpointsEnabled": true
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

}
