package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication

import java.{util => ju}

import play.api.http.HeaderNames._
import play.api.http.MimeTypes._
import play.api.http.Status._
import play.api.libs.ws.WSClient
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.{ApplicationId, Environment}
import uk.gov.hmrc.apiplatformmicroservice.subscriptionfields.{SubscriptionFieldDefinitionsMock, SubscriptionFieldValuesMock}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.ClientId
import uk.gov.hmrc.apiplatformmicroservice.utils.WireMockSpec

class ThirdPartyApplicationSpec extends WireMockSpec with ApplicationMock with SubscriptionFieldDefinitionsMock with SubscriptionFieldValuesMock {

  "WireMock" should {
    val wsClient = app.injector.instanceOf[WSClient]

    "stub get request for fetching an application" in {
      val clientId = ClientId(ju.UUID.randomUUID.toString)
      val applicationId = ApplicationId.random
      mockFetchApplication(Environment.PRODUCTION, applicationId, clientId)
      mockFetchApplicationNotFound(Environment.SANDBOX, applicationId)
      mockFetchApplicationSubscriptions(Environment.PRODUCTION, applicationId)
      mockBulkFetchFieldValuesAndDefinitions(Environment.PRODUCTION, clientId)

      val response = await(wsClient.url(s"$baseUrl/applications/${applicationId.value}")
        .withHttpHeaders(ACCEPT -> JSON)
        .get())

      response.status shouldBe OK
    }
  }
}
