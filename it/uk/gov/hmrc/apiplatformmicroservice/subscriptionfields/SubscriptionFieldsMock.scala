package uk.gov.hmrc.apiplatformmicroservice.subscriptionfields

import com.github.tomakehurst.wiremock.client.WireMock._

import play.api.http.Status._
import play.api.http._

trait SubscriptionFieldsMock {

  def mockBulkFetchFieldDefintions() {
    stubFor(get(urlEqualTo("/definition"))
      .willReturn(
        aResponse()
          .withBody("""{
                      | "apis" : []
                      |}""".stripMargin)
          .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
          .withStatus(OK)
      ))
  }

}
