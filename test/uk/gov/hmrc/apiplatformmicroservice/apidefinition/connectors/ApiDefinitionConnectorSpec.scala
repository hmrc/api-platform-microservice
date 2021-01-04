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

import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.mocks.ApiDefinitionHttpMockingHelper
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.APICategoryDetails
import uk.gov.hmrc.apiplatformmicroservice.util.AsyncHmrcSpec
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.http.ws.WSGet

import scala.concurrent.ExecutionContext.Implicits.global

class ApiDefinitionConnectorSpec extends AsyncHmrcSpec with DefinitionsFromJson {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  val UpstreamInternalServerError = UpstreamErrorResponse("Internal server error", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)

  val bearer = "TestBearerToken"
  val apiKeyTest = UUID.randomUUID().toString

  val serviceName = "someService"
  val userEmail = "3rdparty@example.com"

  val apiName1 = "Calendar"
  val apiName2 = "HelloWorld"

  trait PrincipalSetup extends ApiDefinitionHttpMockingHelper {
    import PrincipalApiDefinitionConnector._
    val apiDefinitionUrl = "/mockUrl"
    val config = Config(baseUrl = apiDefinitionUrl)

    val mockHttpClient = mock[HttpClient with WSGet]

    override val mockThisClient = mockHttpClient

    val connector = new PrincipalApiDefinitionConnector(mockHttpClient, config)

  }

  "principal api definition connector" should {
    "when requesting an api definition" should {

      "call the underlying http client" in new PrincipalSetup {
        whenGetDefinition(serviceName)(apiDefinition(apiName1))

        val result = await(connector.fetchApiDefinition(serviceName))

        result should be('defined)
        result.head.name shouldEqual apiName1
      }

      "throw an exception correctly" in new PrincipalSetup {
        whenGetDefinitionFails(serviceName)(UpstreamInternalServerError)

        intercept[UpstreamErrorResponse] {
          await(connector.fetchApiDefinition(serviceName))
        }
      }

      "return none when nothing found" in new PrincipalSetup {
        whenGetDefinitionFindsNothing(serviceName)

        val result = await(connector.fetchApiDefinition(serviceName))
        result should not be 'defined
      }
    }

    "when requesting all api definitions" should {

      "call the underlying http client with the type argument set to all" in new PrincipalSetup {
        whenGetAllDefinitions(apiDefinition(apiName1), apiDefinition(apiName2))

        val result = await(connector.fetchAllApiDefinitions)

        result.size shouldEqual 2
        result.map(_.name) shouldEqual List(apiName1, apiName2)
      }

      "do not throw exception when not found but instead return empty seq" in new PrincipalSetup {
        whenGetAllDefinitionsFindsNothing()

        val result = await(connector.fetchAllApiDefinitions)
        result shouldEqual List.empty
      }

      "throw an exception correctly" in new PrincipalSetup {
        whenGetAllDefinitionsFails(UpstreamInternalServerError)

        intercept[UpstreamErrorResponse] {
          await(connector.fetchAllApiDefinitions)
        }.statusCode shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "when requesting API Category details" should {
      val category1 = APICategoryDetails("API_CATEGORY_1", "API Category 1")
      val category2 = APICategoryDetails("API_CATEGORY_2", "API Category 2")

      "call the underlying http client" in new PrincipalSetup {
        whenGetAPICategoryDetails()(category1, category2)

        val result = await(connector.fetchApiCategoryDetails())

        result should contain only (category1, category2)
      }

      "throw an exception correctly" in new PrincipalSetup {
        whenGetAPICategoryDetailsFails(UpstreamInternalServerError)

        intercept[UpstreamErrorResponse] {
          await(connector.fetchApiCategoryDetails())
        }.statusCode shouldBe INTERNAL_SERVER_ERROR
      }

    }
  }
}
