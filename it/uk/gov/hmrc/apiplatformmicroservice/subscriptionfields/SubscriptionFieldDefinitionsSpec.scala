package uk.gov.hrmc.apiplatformmicroservice.subscriptionfields

import scala.concurrent.ExecutionContext.Implicits.global

import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import play.api.http.Status._
import org.mockito.MockitoSugar
import org.mockito.ArgumentMatchersSugar
import org.scalatest.OptionValues
import org.scalatestplus.play.WsScalaTestClient
import play.api.test.DefaultAwaitTimeout
import play.api.test.FutureAwaits
import play.api.Configuration
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.Mode
import uk.gov.hmrc.apiplatformmicroservice.subscriptionfields.SubscriptionFieldsMock
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.ws.WSClient
import play.api.http.HeaderNames._
import play.api.http.MimeTypes._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.Environment

class SubscriptionFieldDefinitionsSpec
    extends WordSpec
    with Matchers
    with OptionValues
    with WsScalaTestClient
    with MockitoSugar
    with ArgumentMatchersSugar
    with DefaultAwaitTimeout
    with FutureAwaits
    with BeforeAndAfterEach
    with GuiceOneServerPerSuite
    with SubscriptionFieldsMock {

  val WireMockHost = "localhost"
  val WireMockPort = 11111
  val wireMockServer = new WireMockServer(wireMockConfig().port(WireMockPort))
  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val stubConfig = Configuration(
    "microservice.services.api-definition-principal.host" -> WireMockHost,
    "microservice.services.api-definition-subordinate.host" -> WireMockHost,
    "microservice.services.third-party-application-principal.host" -> WireMockHost,
    "microservice.services.third-party-application-subordinate.host" -> WireMockHost,
    "microservice.services.subscription-fields-principal.host" -> WireMockHost,
    "microservice.services.subscription-fields-subordinate.host" -> WireMockHost,
    "microservice.services.api-definition-principal.port" -> WireMockPort,
    "microservice.services.api-definition-subordinate.port" -> WireMockPort,
    "microservice.services.third-party-application-principal.port" -> WireMockPort,
    "microservice.services.third-party-application-subordinate.port" -> WireMockPort,
    "microservice.services.subscription-fields-principal.port" -> WireMockPort,
    "microservice.services.subscription-fields-subordinate.port" -> WireMockPort,
    "metrics.jvm" -> false
  )

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(stubConfig)
      .in(Mode.Test)
      .build()

  override lazy val port = 8080

  lazy val baseUrl = s"http://localhost:$port"

  override def beforeEach {
    wireMockServer.start()
    WireMock.configureFor(WireMockHost, WireMockPort)
  }

  override def afterEach {
    wireMockServer.stop()
  }

  "WireMock" should {
    val wsClient = app.injector.instanceOf[WSClient]

    "stub get request" in {
      mockBulkFetchFieldDefintions()
      val responseFuture = await(wsClient.url(s"$baseUrl/subscription-fields")
        .withQueryStringParameters("environment" -> Environment.PRODUCTION.toString())
        .withHttpHeaders(ACCEPT -> JSON)
        .get())

      responseFuture.status shouldBe OK
    }
  }
}
