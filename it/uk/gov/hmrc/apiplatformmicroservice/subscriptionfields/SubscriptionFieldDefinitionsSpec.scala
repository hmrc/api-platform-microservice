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
import play.api.http.HeaderNames
import uk.gov.hmrc.http.HeaderCarrier

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

  val Host = "localhost"
  val Port = 11111
  val wireMockServer = new WireMockServer(wireMockConfig().port(Port))
  implicit val hc: HeaderCarrier = HeaderCarrier()
  val baseUrl = "http://localhost:19001"

  private val stubConfig = Configuration(
    "microservice.services.api-definition-principal.host" -> Host,
    "microservice.services.api-definition-subordinate.host" -> Host,
    "microservice.services.third-party-application-principal.host" -> Host,
    "microservice.services.third-party-application-subordinate.host" -> Host,
    "microservice.services.subscription-fields-principal.host" -> Host,
    "microservice.services.subscription-fields-subordinate.host" -> Host,
    "microservice.services.api-definition-principal.port" -> Port,
    "microservice.services.api-definition-subordinate.port" -> Port,
    "microservice.services.third-party-application-principal.port" -> Port,
    "microservice.services.third-party-application-subordinate.port" -> Port,
    "microservice.services.subscription-fields-principal.port" -> Port,
    "microservice.services.subscription-fields-subordinate.port" -> Port,
    "metrics.jvm" -> false
  )

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(stubConfig)
      .in(Mode.Test)
      .build()

  override def beforeEach {
    wireMockServer.start()
    WireMock.configureFor(Host, Port)
  }

  override def afterEach {
    wireMockServer.stop()
  }

  "WireMock" should {
    // val http = app.injector.instanceOf[HttpClient]
    val wsClient = app.injector.instanceOf[WSClient]

    "stub get request" in {
      mockBulkFetchFieldDefintions()
      val responseFuture = await(wsClient.url(s"$baseUrl/subscription-fields")
        .withQueryStringParameters("environment" -> "PRODUCTION")
        .withHttpHeaders(HeaderNames.ACCEPT -> "application/json")
        .get())

      responseFuture.status shouldBe OK
    }
  }
}
