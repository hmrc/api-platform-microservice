package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication

import play.api.http.Status._
import play.api.libs.ws.WSClient
import play.api.http.HeaderNames._
import play.api.http.MimeTypes._
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.Environment
import uk.gov.hmrc.apiplatformmicroservice.subscriptionfields.ApplicationMock
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.ApplicationId
import java.{util => ju}
import uk.gov.hmrc.apiplatformmicroservice.utils.WireMockSpec
import uk.gov.hmrc.apiplatformmicroservice.subscriptionfields.SubscriptionFieldDefinitionsMock
import uk.gov.hmrc.apiplatformmicroservice.subscriptionfields.SubscriptionFieldValuesMock
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.ClientId

class ThirdPartyApplicationSpec extends WireMockSpec with ApplicationMock with SubscriptionFieldDefinitionsMock with SubscriptionFieldValuesMock {

  "WireMock" should {
    val wsClient = app.injector.instanceOf[WSClient]

    "stub get request" in {
      val clientId = ClientId(ju.UUID.randomUUID.toString)
      val applicationId = ApplicationId(ju.UUID.randomUUID.toString)

      mockFetchApplication(Environment.PRODUCTION, applicationId, clientId)
      mockFetchApplicationNotFound(Environment.SANDBOX, applicationId)
      mockFetchApplicationSubscriptions(Environment.PRODUCTION, applicationId)
      mockBulkFetchFieldValues(Environment.PRODUCTION, clientId)

      val response = await(wsClient.url(s"$baseUrl/applications/${applicationId.value}")
        .withHttpHeaders(ACCEPT -> JSON)
        .get())

      response.status shouldBe OK
    }
  }
}
