package uk.gov.hmrc.apiplatformmicroservice.utils

import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}
import org.mockito.MockitoSugar
import org.mockito.ArgumentMatchersSugar
import org.scalatest.OptionValues
import org.scalatestplus.play.WsScalaTestClient
import play.api.test.DefaultAwaitTimeout
import play.api.test.FutureAwaits
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.play.FakeApplicationFactory
import org.scalatest.BeforeAndAfterAll

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
