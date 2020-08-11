/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.apiplatformmicroservice.common

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import uk.gov.hmrc.apiplatformmicroservice.util.AsyncHmrcSpec
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class ServicesConfigBridgeExtensionSpec extends AsyncHmrcSpec with MockitoSugar with ArgumentMatchersSugar {

  trait Setup {

    val proxiedServiceName = "PROXY"
    val directServiceName = "DIRECT"
    val baseUrl = "BASE"
    val proxySuffix = "SUFFIX"
    val key = "SOME_KEY"

    val servicesConfig = mock[ServicesConfig]

    val extension = new ServicesConfigBridgeExtension {
      val sc = servicesConfig
    }

    when(servicesConfig.getConfBool(eqTo(s"$proxiedServiceName.use-proxy"), any)).thenReturn(true)
    when(servicesConfig.getConfBool(eqTo(s"$directServiceName.use-proxy"), any)).thenReturn(false)

    when(servicesConfig.getConfString(s"$proxiedServiceName.context", key)).thenReturn(proxySuffix)

    when(servicesConfig.baseUrl(any)).thenReturn(baseUrl)
  }

  "ServicesConfigBridgeExtension" when {
    "useProxy is called" should {
      "return true if the proxy is enabled in config" in new Setup {
        extension.useProxy(proxiedServiceName) shouldBe true
      }
      "return false if the proxy is not enabled in config" in new Setup {
        extension.useProxy(directServiceName) shouldBe false
      }
    }

    "serviceBaseUrl" should {
      "use base url and context values when proxy is enabled" in new Setup {
        extension.serviceUrl(key)(proxiedServiceName) shouldBe s"$baseUrl/$proxySuffix"
      }
    }
  }
}
