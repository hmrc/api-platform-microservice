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
