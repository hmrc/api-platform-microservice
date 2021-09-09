package uk.gov.hmrc.apiplatformmicroservice.utils

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatest._
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.play.{FakeApplicationFactory, WsScalaTestClient}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.ServerProvider

trait WireMockSpec
    extends AnyWordSpec
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
    with PrincipalAndSubordinateWireMockSetup
    with ServerProvider {

  lazy val baseUrl = s"http://localhost:$port"
}
