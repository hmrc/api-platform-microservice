package uk.gov.hmrc.apiplatformmicroservice.subscriptionfields

import com.github.tomakehurst.wiremock.client.WireMock._

import play.api.http.Status._
import play.api.http._
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.Environment

trait ApplicationMock {

  def mockFetchApplication(applicationId: ApplicationId, deployedTo: Environment) {
    stubFor(get(urlEqualTo(s"/application/${applicationId.value}"))
      .willReturn(
        aResponse()
          .withBody(s"""{
                      |  "id": "${applicationId.value}",
                      |  "clientId": "dummyProdId",
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
                      |  "trusted": false
                      |}""".stripMargin)
          .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
          .withStatus(OK)
      ))
  }

}
