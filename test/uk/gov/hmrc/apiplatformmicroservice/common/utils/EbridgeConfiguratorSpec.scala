/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.apiplatformmicroservice.common.utils

import uk.gov.hmrc.http.client.RequestBuilder

class EbridgeConfiguratorSpec extends AsyncHmrcSpec {

  trait Setup {
    val bearerToken    = "bearer-token"
    val apiKey         = "api-key"
    val requestBuilder = mock[RequestBuilder]

    when(requestBuilder.withProxy).thenReturn(requestBuilder)
    when(requestBuilder.setHeader(*[(String, String)])).thenReturn(requestBuilder)
  }

  "EbridgeConfigurator" when {
    "using configure" should {
      "add a proxy and all headers when useProxy is true" in new Setup {
        EbridgeConfigurator.configure(useProxy = true, bearerToken, apiKey)(requestBuilder)

        verify(requestBuilder, times(1)).withProxy
        verify(requestBuilder, times(1)).setHeader(
          eqTo(("Authorization", s"Bearer $bearerToken")),
          eqTo(("x-api-key", apiKey))
        )
      }

      "add a proxy and some headers when useProxy is true and apiKey is not supplied" in new Setup {
        EbridgeConfigurator.configure(useProxy = true, bearerToken, "")(requestBuilder)

        verify(requestBuilder, times(1)).withProxy
        verify(requestBuilder, times(1)).setHeader(
          eqTo(("Authorization", s"Bearer $bearerToken"))
        )
      }

      "not add a proxy or headers to RequestBuilder when useProxy is false" in new Setup {
        EbridgeConfigurator.configure(useProxy = false, bearerToken, apiKey)(requestBuilder)

        verify(requestBuilder, never).withProxy
        verify(requestBuilder, never).setHeader(*)
      }
    }
  }
}
