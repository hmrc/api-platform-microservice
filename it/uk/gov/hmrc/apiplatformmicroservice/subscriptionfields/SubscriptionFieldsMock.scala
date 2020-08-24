package uk.gov.hmrc.apiplatformmicroservice.subscriptionfields

import com.github.tomakehurst.wiremock.client.WireMock._

import play.api.http.Status._
import play.api.http._

trait SubscriptionFieldsMock {
  def Port: Int
  def Host: String

  def mockBulkFetchFieldDefintions() {
    stubFor(get(urlEqualTo(s"/definition"))
      .willReturn(
        aResponse()
          .withBody("""{"apis": []}""")
          .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
          .withStatus(OK)
      ))
  }

}
