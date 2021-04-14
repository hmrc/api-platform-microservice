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

package uk.gov.hmrc.apiplatformmicroservice.apidefinition.connectors

import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import play.api.Environment
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.mocks.ApiDefinitionHttpMockingHelper
import uk.gov.hmrc.apiplatformmicroservice.common.ProxiedHttpClient
import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.http.ws.WSGet

import scala.concurrent.ExecutionContext.Implicits.global

class SubordinateApiDefinitionConnectorSpec extends AsyncHmrcSpec with DefinitionsFromJson {
  private val environmentName = "ENVIRONMENT"

  private val futureTimeoutSupport = new FutureTimeoutSupportImpl

  implicit val hc: HeaderCarrier = HeaderCarrier()
  private val UpstreamException = UpstreamErrorResponse("Internal server error", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)

  private val bearer = "TestBearerToken"
  private val apiKeyTest = UUID.randomUUID().toString

  private val serviceName = "someService"

  private val apiName1 = "Calendar"
  private val apiName2 = "HelloWorld"

  class Setup(proxyEnabled: Boolean = true) extends ApiDefinitionHttpMockingHelper {

    private implicit val actorSystemTest: ActorSystem = ActorSystem(
      "test-actor-system"
    )
    private implicit val materializer: ActorMaterializer = ActorMaterializer()

    val apiDefinitionUrl = "/mockUrl"

    val config = SubordinateApiDefinitionConnector.Config(
      serviceBaseUrl = apiDefinitionUrl,
      useProxy = proxyEnabled,
      bearerToken = bearer,
      apiKey = apiKeyTest
    )

    val mockEnvironment: Environment = mock[Environment]
    when(mockEnvironment.toString).thenReturn(environmentName)

    val mockProxiedHttpClient: ProxiedHttpClient = mock[ProxiedHttpClient]
    when(mockProxiedHttpClient.withHeaders(any, any))
      .thenReturn(mockProxiedHttpClient)

    val mockHttpClient: HttpClient with WSGet = mock[HttpClient with WSGet]

    override val mockThisClient: ProxiedHttpClient = mockProxiedHttpClient

    val connector = new SubordinateApiDefinitionConnector(
      config,
      mockHttpClient,
      mockProxiedHttpClient,
      actorSystemTest,
      futureTimeoutSupport
    )

  }

  "subordinate connector" should {
    "when requesting an api definition" should {

      "call the underlying http client" in new Setup {
        whenGetDefinition(serviceName)(apiDefinition(apiName1))

        val result = await(connector.fetchApiDefinition(serviceName))

        result should be('defined)
        result.head.name shouldEqual apiName1
      }

      "throw an exception correctly" in new Setup {
        whenGetDefinitionFails(serviceName)(UpstreamException)

        intercept[UpstreamException.type] {
          await(connector.fetchApiDefinition(serviceName))
        }
      }

      "do not throw exception when not found but instead return None" in new Setup {
        whenGetDefinitionFindsNothing(serviceName)

        val result = await(connector.fetchApiDefinition(serviceName))
        result should not be 'defined
      }
    }

    "when requesting all api definitions" should {
      "call the underlying http client" in new Setup {
        whenGetAllDefinitions(apiDefinition(apiName1), apiDefinition(apiName2))

        val result =
          await(connector.fetchAllApiDefinitions)

        result.size shouldEqual 2
        result.map(_.name) shouldEqual List(apiName1, apiName2)
      }

      "do not throw exception when not found but instead return empty list" in new Setup {
        whenGetAllDefinitionsFindsNothing()

        val result =
          await(connector.fetchAllApiDefinitions)
        result shouldEqual List.empty
      }

      "throw an exception correctly" in new Setup {
        whenGetAllDefinitionsFails(UpstreamException)

        intercept[UpstreamException.type] {
          await(connector.fetchAllApiDefinitions)
        }
      }
    }

    "for http" when {
      "configured not to use the proxy" should {
        "use the HttpClient" in new Setup(proxyEnabled = false) {
          connector.http shouldEqual mockHttpClient
        }
      }

      "configured to use the proxy" should {
        "use the ProxiedHttpClient with the correct authorisation" in new Setup(
          proxyEnabled = true
        ) {
          connector.http shouldEqual mockProxiedHttpClient

          verify(mockProxiedHttpClient).withHeaders(bearer, apiKeyTest)
        }
      }
    }
  }
}
