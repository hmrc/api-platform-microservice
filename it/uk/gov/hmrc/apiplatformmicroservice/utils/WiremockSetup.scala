package uk.gov.hmrc.apiplatformmicroservice.utils

import com.github.tomakehurst.wiremock.WireMockServer
import uk.gov.hmrc.http.HeaderCarrier
import play.api.Application
import play.api.Mode
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import org.scalatestplus.play.FakeApplicationFactory
import play.api.inject.guice.GuiceApplicationBuilder
import org.scalatest.BeforeAndAfterEach

trait WiremockSetup {
  self: ConfigBuilder with FakeApplicationFactory with BeforeAndAfterEach =>

  val WireMockHost = "localhost"
  val WireMockPort = 11111
  val wireMockServer = new WireMockServer(wireMockConfig().port(WireMockPort))
  implicit val hc: HeaderCarrier = HeaderCarrier()

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(stubConfig(WireMockHost, WireMockPort))
      .in(Mode.Test)
      .build()

  override def beforeEach() {
    wireMockServer.start()
    WireMock.configureFor(WireMockHost, WireMockPort)
  }

  override def afterEach() {
    wireMockServer.stop()
  }

}
