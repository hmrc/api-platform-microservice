/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors

import uk.gov.hmrc.apiplatformmicroservice.common.ProxiedHttpClient
import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global

import uk.gov.hmrc.http.HeaderCarrier
class ThirdPartyApplicationConnectorSpec extends AsyncHmrcSpec {

  private val baseUrl = "https://example.com"

  class Setup(proxyEnabled: Boolean = false) {
    implicit val hc = HeaderCarrier()
    protected val mockHttpClient = mock[HttpClient]
    protected val mockProxiedHttpClient = mock[ProxiedHttpClient]
    val apiKeyTest = "5bb51bca-8f97-4f2b-aee4-81a4a70a42d3"
    val bearer = "TestBearerToken"

    val connector = new AbstractThirdPartyApplicationConnector {
      val httpClient = mockHttpClient
      val proxiedHttpClient = mockProxiedHttpClient

      val config = AbstractThirdPartyApplicationConnector.Config(
        baseUrl,
        proxyEnabled,
        bearer,
        apiKeyTest
      )
    }
  }

  class SubordinateSetup(proxyEnabled: Boolean = false) extends Setup(proxyEnabled) {

    val config = AbstractThirdPartyApplicationConnector.Config(
      baseUrl,
      proxyEnabled,
      bearer,
      apiKeyTest
    )

    override val connector = new SubordinateThirdPartyApplicationConnector(
      config,
      mockHttpClient,
      mockProxiedHttpClient
    ) {}
  }

  "http" when {
    "configured not to use the proxy" should {
      "use the HttpClient" in new Setup(proxyEnabled = false) {
        connector.http shouldBe mockHttpClient
      }
    }

    "configured to use the proxy" should {
      "use the ProxiedHttpClient with the correct authorisation" in new Setup(proxyEnabled = true) {
        when(mockProxiedHttpClient.withHeaders(bearer, apiKeyTest)).thenReturn(mockProxiedHttpClient)

        connector.http shouldBe mockProxiedHttpClient

        verify(mockProxiedHttpClient).withHeaders(bearer, apiKeyTest)
      }
    }
  }

}
