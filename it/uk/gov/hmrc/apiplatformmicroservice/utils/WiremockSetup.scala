package uk.gov.hmrc.apiplatformmicroservice.utils

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.{MappingBuilder, WireMock}
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.FakeApplicationFactory
import play.api.{Application, Mode}
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.Environment
import uk.gov.hmrc.http.HeaderCarrier

trait WiremockSetup {
  self: ConfigBuilder with FakeApplicationFactory with BeforeAndAfterEach with BeforeAndAfterAll =>

  val WireMockHost = "localhost"
  val WireMockPrincipalPort = 11111
  val WireMockSubordinatePort = 22222

  val wireMockPrincipalServer = new WireMockServer(wireMockConfig().port(WireMockPrincipalPort))
  val wireMockSubordinateServer = new WireMockServer(wireMockConfig().port(WireMockSubordinatePort))

  val principalWireMock = WireMock.create().host(WireMockHost).port(WireMockPrincipalPort).build()
  val subordinateWireMock = WireMock.create().host(WireMockHost).port(WireMockSubordinatePort).build()

  def stubFor(environment: Environment = Environment.PRODUCTION)(mappingBuilder: MappingBuilder): StubMapping = environment match {
    case Environment.PRODUCTION => principalWireMock.register(mappingBuilder)
    case _                      => subordinateWireMock.register(mappingBuilder)
  }

  def stubForProd: MappingBuilder => StubMapping = stubFor(Environment.PRODUCTION)

  def stubForSandbox: MappingBuilder => StubMapping = stubFor(Environment.SANDBOX)

  implicit val hc: HeaderCarrier = HeaderCarrier()

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(stubConfig(WireMockPrincipalPort, WireMockSubordinatePort))
      .in(Mode.Test)
      .build()

  override def beforeAll() {
    wireMockPrincipalServer.start()
    wireMockSubordinateServer.start()
  }

  override def beforeEach() {
    principalWireMock.resetMappings()
    subordinateWireMock.resetMappings()
  }

  override def afterAll() {
    wireMockPrincipalServer.stop()
    wireMockSubordinateServer.stop()
  }

}
