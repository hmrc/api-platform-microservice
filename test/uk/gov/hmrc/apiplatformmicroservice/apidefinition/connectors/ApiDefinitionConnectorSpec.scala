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

package uk.gov.hmrc.apiplatformmicroservice.apidefinition.connectors

import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito._
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.connectors.ApiDefinitionConnector.ApiDefinitionConnectorConfig
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.{APIDefinition, ApiDefinitionTestDataHelper}
import uk.gov.hmrc.apiplatformmicroservice.util.AsyncHmrcSpec
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Future.successful

class ApiDefinitionConnectorSpec extends AsyncHmrcSpec with ApiDefinitionTestDataHelper {

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val mockHttp: HttpClient = mock[HttpClient]
    val baseUrl = "http://api-definition"
    val config = ApiDefinitionConnectorConfig(baseUrl)
    def endpoint(path: String) = s"$baseUrl/$path"
    val helloApiDefinition = apiDefinition("hello-api")

    val connector = new ApiDefinitionConnector(mockHttp, config)
  }

  "fetchAllApiDefinitions" should {
    "return API definitions" in new Setup {
      when(mockHttp.GET[Seq[APIDefinition]](meq(endpoint("api-definition")), meq(Seq("filterApis" -> "false")))(any(), any(), any()))
        .thenReturn(successful(Seq(helloApiDefinition)))

      val result: Seq[APIDefinition] = await(connector.fetchAllApiDefinitions)

      result shouldBe Seq(helloApiDefinition)
    }

    "propagate error when endpoint returns error" in new Setup {
      val expectedException = "something went wrong"
      when(mockHttp.GET[Seq[APIDefinition]](meq(endpoint("api-definition")), meq(Seq("filterApis" -> "false")))(any(), any(), any()))
        .thenReturn(Future.failed(new RuntimeException(expectedException)))

      val ex = intercept[RuntimeException] {
        await(connector.fetchAllApiDefinitions)
      }

      ex.getMessage shouldBe expectedException
    }
  }
}
