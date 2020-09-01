package uk.gov.hmrc.apiplatformmicroservice.utils

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatest._
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.play.{FakeApplicationFactory, WsScalaTestClient}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}

trait WireMockSpec
    extends WordSpec
    with Matchers
    with OptionValues
    with WsScalaTestClient
    with MockitoSugar
    with ArgumentMatchersSugar
    with DefaultAwaitTimeout
    with FutureAwaits
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with GuiceOneServerPerSuite
    with FakeApplicationFactory
    with ConfigBuilder
    with WiremockSetup {

  override lazy val port = 8080

  lazy val baseUrl = s"http://localhost:$port"
}
