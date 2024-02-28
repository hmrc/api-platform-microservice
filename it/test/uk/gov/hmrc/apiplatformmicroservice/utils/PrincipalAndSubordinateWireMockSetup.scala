/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.apiplatformmicroservice.utils

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.{MappingBuilder, WireMock}
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}
import org.scalatestplus.play.FakeApplicationFactory

import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Mode}
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.common.domain.models.Environment

trait PrincipalAndSubordinateWireMockSetup extends BeforeAndAfterEach with BeforeAndAfterAll {
  self: Suite with ConfigBuilder with FakeApplicationFactory =>

  val WireMockHost            = "localhost"
  val WireMockPrincipalPort   = 11111
  val WireMockSubordinatePort = 22222

  val wireMockPrincipalServer   = new WireMockServer(wireMockConfig().port(WireMockPrincipalPort))
  val wireMockSubordinateServer = new WireMockServer(wireMockConfig().port(WireMockSubordinatePort))

  val principalWireMock   = WireMock.create().host(WireMockHost).port(WireMockPrincipalPort).build()
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

  override def beforeAll(): Unit = {
    wireMockPrincipalServer.start()
    wireMockSubordinateServer.start()
  }

  override def beforeEach(): Unit = {
    principalWireMock.resetMappings()
    subordinateWireMock.resetMappings()
  }

  override def afterAll(): Unit = {
    wireMockPrincipalServer.stop()
    wireMockSubordinateServer.stop()
  }

}
