package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors

import uk.gov.hmrc.apiplatformmicroservice.common.utils.{AsyncHmrcSpec, WireMockSugar, WireMockSugarExtensions}
import uk.gov.hmrc.apiplatformmicroservice.common.builder.UserResponseBuilder
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.http.HeaderCarrier
import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.Status._
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.UserId
import uk.gov.hmrc.http.UpstreamErrorResponse

class ThirdPartyDeveloperConnectorSpec
    extends AsyncHmrcSpec 
    with WireMockSugar 
    with WireMockSugarExtensions 
    with GuiceOneServerPerSuite
    with UserResponseBuilder {

  trait Setup {
    implicit val hc = HeaderCarrier()

    val httpClient = app.injector.instanceOf[HttpClient]
    val mockEncryptedJson = mock[EncryptedJson]

    val mockConfig: ThirdPartyDeveloperConnector.Config = mock[ThirdPartyDeveloperConnector.Config]
    when(mockConfig.applicationBaseUrl).thenReturn(wireMockUrl)

    val tpdConnector = new ThirdPartyDeveloperConnector(mockConfig, httpClient, mockEncryptedJson)
  }

  "fetchByEmail" should {
    val url = "/developers/get-by-emails"

    "respond with 200 and data" in new Setup {
      val fakeUser1 = buildUserResponse(UserId.random, "fakeemail1", true)
      val fakeUser2 = buildUserResponse(UserId.random, "fakeemail2", true)

      stubFor(
        post(urlEqualTo(url))
        .withJsonRequestBody(List("fakeemail1", "fakeemail2"))
        .willReturn(
          aResponse()
          .withStatus(OK)
          .withJsonBody(List(fakeUser1, fakeUser2))
        )
      )

      val result = await(tpdConnector.fetchByEmails(Set("fakeemail1", "fakeemail2")))

      result.toList should contain allOf(fakeUser1, fakeUser2)
    }
    
    "respond with BAD_REQUEST when no email addresses provided" in new Setup {
      stubFor(
        post(urlEqualTo(url))
        .withJsonRequestBody(List.empty)
        .willReturn(
          aResponse()
          .withStatus(BAD_REQUEST)
        )
      )

      intercept[UpstreamErrorResponse] {
        await(tpdConnector.fetchByEmails(Set.empty))
      }.statusCode shouldBe BAD_REQUEST
    }
  }
}