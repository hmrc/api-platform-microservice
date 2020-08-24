package uk.gov.hrmc.apiplatformmicroservice.subscriptionfields

import scala.concurrent.ExecutionContext.Implicits.global

import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{status => wmStatus, _}
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.http.HeaderCarrier
import play.api.test.Helpers.await
import play.api.http.Status._
import org.mockito.MockitoSugar
import org.mockito.ArgumentMatchersSugar
import org.scalatest.OptionValues
import org.scalatestplus.play.WsScalaTestClient
import play.api.test.DefaultAwaitTimeout
import play.api.test.FutureAwaits

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
    with GuiceOneAppPerSuite {

  val Port = 11111
  val Host = "localhost"
  val wireMockServer = new WireMockServer(wireMockConfig().port(Port))
  val http = fakeApplication().injector.instanceOf[HttpClient]
  implicit val hc: HeaderCarrier = HeaderCarrier()

  override def beforeEach {
    wireMockServer.start()
    WireMock.configureFor(Host, Port)
  }

  override def afterEach {
    wireMockServer.stop()
  }

  "WireMock" should {
    "stub get request" in {
      val path = "/my/resource"
      stubFor(get(urlEqualTo(path))
        .willReturn(
          aResponse()
            .withBody("bob")
            .withStatus(OK)
        ))

      val responseFuture = await(http.GET(s"http://$Host:$Port$path"))

      responseFuture.status shouldBe OK
    }
  }
}
