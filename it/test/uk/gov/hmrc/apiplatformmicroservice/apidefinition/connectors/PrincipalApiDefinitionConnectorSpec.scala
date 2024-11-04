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

package uk.gov.hmrc.apiplatformmicroservice.apidefinition.connectors

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

import org.scalatestplus.play.guice.GuiceOneServerPerSuite

import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.http.client.HttpClientV2

import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApiVersionNbr, Environment}
import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ServiceName
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.ApiDefinitionMock
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.{ApiEventId, DisplayApiEvent}
import uk.gov.hmrc.apiplatformmicroservice.common.builder.DefinitionsFromJson
import uk.gov.hmrc.apiplatformmicroservice.common.utils.{AsyncHmrcSpec, WireMockSugarExtensions}
import uk.gov.hmrc.apiplatformmicroservice.utils.{ConfigBuilder, PrincipalAndSubordinateWireMockSetup}

class PrincipalApiDefinitionConnectorSpec
    extends AsyncHmrcSpec
    with WireMockSugarExtensions
    with GuiceOneServerPerSuite
    with DefinitionsFromJson
    with ConfigBuilder
    with PrincipalAndSubordinateWireMockSetup
    with ApiDefinitionMock
    with FixedClock {

  val UpstreamInternalServerError = UpstreamErrorResponse("Internal server error", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)

  val bearer     = "TestBearerToken"
  val apiKeyTest = UUID.randomUUID().toString

  val serviceName = ServiceName("someService")
  val version     = ApiVersionNbr("1.0")
  val userEmail   = "3rdparty@example.com"

  val apiName1 = "Calendar"
  val apiName2 = "HelloWorld"

  trait Setup {
    import PrincipalApiDefinitionConnector._
    val apiDefinitionUrl = s"http://$WireMockHost:$WireMockPrincipalPort"
    val config           = Config(baseUrl = apiDefinitionUrl)

    val httpClient = app.injector.instanceOf[HttpClientV2]

    val connector = new PrincipalApiDefinitionConnector(httpClient, config)
  }

  "principal api definition connector" should {
    "when requesting an api definition" should {

      "call the underlying http client" in new Setup {
        whenGetDefinition(Environment.PRODUCTION)(serviceName, apiDefinition(apiName1))

        val result = await(connector.fetchApiDefinition(serviceName))

        result shouldBe Symbol("defined")
        result.head.name shouldEqual apiName1
      }

      "throw an exception correctly" in new Setup {
        whenGetDefinitionFails(Environment.PRODUCTION)(serviceName, 500)

        intercept[UpstreamErrorResponse] {
          await(connector.fetchApiDefinition(serviceName))
        }
      }

      "return none when nothing found" in new Setup {
        whenGetDefinitionFindsNothing(Environment.PRODUCTION)(serviceName)

        val result = await(connector.fetchApiDefinition(serviceName))
        result should not be Symbol("defined")
      }
    }

    "when requesting all api definitions" should {

      "call the underlying http client with the type argument set to all" in new Setup {
        whenGetAllDefinitions(Environment.PRODUCTION)(apiDefinition(apiName1), apiDefinition(apiName2))

        val result = await(connector.fetchAllApiDefinitions)

        result.size shouldEqual 2
        result.map(_.name) shouldEqual List(apiName1, apiName2)
      }

      "do not throw exception when not found but instead return empty List" in new Setup {
        whenGetAllDefinitionsFindsNothing(Environment.PRODUCTION)

        val result = await(connector.fetchAllApiDefinitions)
        result shouldEqual List.empty
      }

      "throw an exception correctly" in new Setup {
        whenGetAllDefinitionsFails(Environment.PRODUCTION)(500)

        intercept[UpstreamErrorResponse] {
          await(connector.fetchAllApiDefinitions)
        }.statusCode shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "fetchApiSpecification" should {
      "call out and get json value" in new Setup {
        val jsValue: JsValue = Json.parse("""{ "x": 1 }""")
        whenFetchApiSpecification(Environment.PRODUCTION)(serviceName, version, jsValue)

        val result = await(connector.fetchApiSpecification(serviceName, version))

        result shouldBe Some(jsValue)
      }
    }
    "call out and get no value" in new Setup {
      whenFetchApiSpecificationFindsNothing(Environment.PRODUCTION)(serviceName, version)

      val result = await(connector.fetchApiSpecification(serviceName, version))

      result shouldBe None
    }

    "when requesting api events" should {

      "call the underlying http client" in new Setup {
        val displayApiEvent = DisplayApiEvent(ApiEventId.random, serviceName, instant, "Api Created", List.empty, None)
        whenGetApiEvents(Environment.PRODUCTION)(serviceName, List(displayApiEvent))

        val result = await(connector.fetchApiEvents(serviceName))

        result shouldEqual List(displayApiEvent)
      }

      "call the underlying http client, requesting to exclude no change events" in new Setup {
        val displayApiEvent = DisplayApiEvent(ApiEventId.random, serviceName, instant, "Api Created", List.empty, None)
        whenGetApiEvents(Environment.PRODUCTION)(serviceName, List(displayApiEvent), includeNoChange = false)

        val result = await(connector.fetchApiEvents(serviceName, includeNoChange = false))

        result shouldEqual List(displayApiEvent)
      }

      "throw an exception correctly" in new Setup {
        whenGetApiEventsFails(Environment.PRODUCTION)(serviceName, 500)

        intercept[UpstreamErrorResponse] {
          await(connector.fetchApiEvents(serviceName))
        }
      }

      "return empty list when no events found" in new Setup {
        whenGetApiEventsFindsNothing(Environment.PRODUCTION)(serviceName)

        val result = await(connector.fetchApiEvents(serviceName))
        result shouldEqual List.empty
      }
    }
  }
}
