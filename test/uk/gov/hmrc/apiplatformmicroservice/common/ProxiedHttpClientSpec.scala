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

package uk.gov.hmrc.apiplatformmicroservice.common

import java.util.UUID

import akka.actor.ActorSystem
import com.typesafe.config.Config

import play.api.libs.ws.{WSClient, WSRequest}
import play.api.{ConfigLoader, Configuration}
import uk.gov.hmrc.http.{Authorization, _}
import uk.gov.hmrc.play.audit.http.HttpAuditing

import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec

class ProxiedHttpClientSpec extends AsyncHmrcSpec {

  private val actorSystem = ActorSystem("test-actor-system")

  trait Setup {
    val apiKey: String            = UUID.randomUUID().toString
    val bearerToken: String       = UUID.randomUUID().toString
    val url                       = "http://example.com"
    val mockConfig: Configuration = mock[Configuration]
    when(mockConfig.underlying).thenReturn(mock[Config])

    val mockHttpAuditing: HttpAuditing = mock[HttpAuditing]
    val mockWsClient: WSClient         = mock[WSClient]
    val mockWSRequest: WSRequest       = mock[WSRequest]
    when(mockWsClient.url(url)).thenReturn(mockWSRequest)

    val underTest = new ProxiedHttpClient(mockConfig, mockHttpAuditing, mockWsClient, actorSystem)
  }

  "withHeaders" should {

    "creates a ProxiedHttpClient with passed in headers" in new Setup {

      private val proxyClient = underTest.withHeaders(bearerToken, apiKey)

      proxyClient.authorization shouldBe Some(Authorization(s"Bearer $bearerToken"))
      proxyClient.apiKeyHeader shouldBe Some(apiKey)
    }

    "when apiKey is empty String, apiKey header is None" in new Setup {

      private val result = underTest.withHeaders(bearerToken, "")

      result.apiKeyHeader shouldBe None
    }

    "when apiKey isn't provided, apiKey header is None" in new Setup {

      private val result = underTest.withHeaders(bearerToken)

      result.apiKeyHeader shouldBe None
    }
  }

  "buildRequest" should {
    "build request" in new Setup {
      when(mockConfig.getOptional[Boolean](eqTo("proxy.proxyRequiredForThisEnvironment"))(*[ConfigLoader[Boolean]])).thenReturn(Some(false))
      when(mockWSRequest.withHttpHeaders(any)).thenReturn(mockWSRequest)
      when(mockWSRequest.addHttpHeaders(any)).thenReturn(mockWSRequest)

      private val result = underTest.buildRequest(url, Seq.empty)

      result shouldBe mockWSRequest
    }
  }
}
