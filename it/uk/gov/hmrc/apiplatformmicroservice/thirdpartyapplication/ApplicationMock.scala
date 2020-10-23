package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http._
import play.api.http.Status._
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.{ApplicationId, Environment}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.ClientId
import uk.gov.hmrc.apiplatformmicroservice.utils.WiremockSetup

trait ApplicationMock {
  self: WiremockSetup => // To allow for stubFor to work with environment

  def mockFetchApplicationNotFound(env: Environment, applicationId: ApplicationId) {
    stubFor(env)(get(urlEqualTo(s"/application/${applicationId.value}"))
      .willReturn(
        aResponse()
          .withStatus(NOT_FOUND)
      ))
  }

  def mockFetchApplication(deployedTo: Environment, applicationId: ApplicationId, clientId: ClientId = ClientId("dummyProdId")) {
    stubFor(deployedTo)(get(urlEqualTo(s"/application/${applicationId.value}"))
      .willReturn(
        aResponse()
          .withBody(s"""{
                       |  "id": "${applicationId.value}",
                       |  "clientId": "${clientId.value}",
                       |  "gatewayId": "w7dwd9GFZX",
                       |  "name": "giu",
                       |  "deployedTo": "$deployedTo",
                       |  "description": "Some test data",
                       |  "collaborators": [
                       |      {
                       |          "emailAddress": "bobby.taxation@digital.hmrc.gov.uk",
                       |          "role": "ADMINISTRATOR"
                       |      }
                       |  ],
                       |  "createdOn": 1504526587272,
                       |  "lastAccess": 1561071600000,
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
                       |  "trusted": false
                       |}""".stripMargin)
          .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
          .withStatus(OK)
      ))
  }

  def mockFetchApplicationSubscriptions(env: Environment, applicationId: ApplicationId) {
    stubFor(env)(get(urlEqualTo(s"/application/${applicationId.value}/subscription"))
      .willReturn(
        aResponse()
          .withBody("""
                      |[
                      |    {
                      |        "name": "Individual Benefits",
                      |        "serviceName": "individual-benefits",
                      |        "context": "individual-benefits",
                      |        "versions": [
                      |            {
                      |                "version": {
                      |                    "version": "1.0",
                      |                    "status": "DEPRECATED",
                      |                    "access": {
                      |                        "type": "PUBLIC"
                      |                    }
                      |                },
                      |                "subscribed": false
                      |            },
                      |            {
                      |                "version": {
                      |                    "version": "1.1",
                      |                    "status": "BETA",
                      |                    "access": {
                      |                        "type": "PUBLIC"
                      |                    }
                      |                },
                      |                "subscribed": false
                      |            }
                      |        ],
                      |        "isTestSupport": false
                      |    },
                      |    {
                      |        "name": "Individual Employment",
                      |        "serviceName": "individual-employment",
                      |        "context": "individual-employment",
                      |        "versions": [
                      |            {
                      |                "version": {
                      |                    "version": "1.0",
                      |                    "status": "DEPRECATED",
                      |                    "access": {
                      |                        "type": "PUBLIC"
                      |                    }
                      |                },
                      |                "subscribed": false
                      |            },
                      |            {
                      |                "version": {
                      |                    "version": "1.1",
                      |                    "status": "BETA",
                      |                    "access": {
                      |                        "type": "PUBLIC"
                      |                    }
                      |                },
                      |                "subscribed": false
                      |            }
                      |        ],
                      |        "isTestSupport": false
                      |    }
                      |]
          """.stripMargin)
          .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
          .withStatus(OK)
      ))
  }
}
