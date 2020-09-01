package uk.gov.hmrc.apiplatformmicroservice.subscriptionfields

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http._
import play.api.http.Status._
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.Environment
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.ClientId
import uk.gov.hmrc.apiplatformmicroservice.utils.WiremockSetup

trait SubscriptionFieldValuesMock {
  self: WiremockSetup => // To allow for stubFor to work with environment

  def mockBulkFetchFieldValues(env: Environment, clientId: ClientId) {
    stubFor(env)(get(urlEqualTo(s"/field/application/${clientId.value}"))
      .willReturn(
        aResponse()
          .withBody(s"""{
                       |  "subscriptions": [
                       |      {
                       |          "clientId": "${clientId.value}",
                       |          "apiContext": "hello",
                       |          "apiVersion": "1.0",
                       |          "fieldsId": "d7b7c67f-0edb-4811-8e1f-69eb3518ced6",
                       |          "fields": {
                       |              "helloworldFieldOne": "a",
                       |              "helloworldFieldTwo": "b",
                       |              "helloworldFieldThree": "c"
                       |          }
                       |      },
                       |      {
                       |          "clientId": "${clientId.value}",
                       |          "apiContext": "customs/declarations",
                       |          "apiVersion": "1.0",
                       |          "fieldsId": "dcdd563d-44e8-4c0c-a841-df4b882edbc9",
                       |          "fields": {
                       |              "callbackUrl": "http://localhost:123/blah",
                       |              "securityToken": "y",
                       |              "authenticatedEori": ""
                       |          }
                       |      }
                       |  ]
                       |}""".stripMargin)
          .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
          .withStatus(OK)
      ))
  }

}
