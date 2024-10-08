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

package uk.gov.hmrc.apiplatformmicroservice.xmlapis.connectors

import scala.concurrent.ExecutionContext.Implicits.global

import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.http.client.HttpClientV2

import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ServiceName
import uk.gov.hmrc.apiplatformmicroservice.utils.WireMockSpec
import uk.gov.hmrc.apiplatformmicroservice.xmlapis.models.XmlApi

class XmlApisConnectorSpec extends WireMockSpec with XmlApisMock {

  trait Setup {
    val httpClient                      = app.injector.instanceOf[HttpClientV2]
    val config: XmlApisConnector.Config = XmlApisConnector.Config(s"http://$WireMockHost:$WireMockPrincipalPort")
    val connector                       = new XmlApisConnector(httpClient, config)

    val xmlApi1: XmlApi = XmlApi(
      name = "xml api 1",
      serviceName = ServiceName("xml-api-1"),
      context = "xml api context",
      description = "xml api description",
      categories = None
    )

    val xmlApi2: XmlApi = xmlApi1.copy(name = "xml api 2")
    val xmlApis         = Seq(xmlApi1, xmlApi2)

  }

  "fetchAllXmlApis" should {
    "return all Xml Apis" in new Setup {
      whenGetAllXmlApis(xmlApis: _*)

      val response = await(connector.fetchAllXmlApis())

      response shouldBe xmlApis
    }

    "throw an exception correctly" in new Setup {
      whenGetAllXmlApisReturnsError(500)
      intercept[UpstreamErrorResponse] {
        await(connector.fetchAllXmlApis())
      }
    }
  }

  "fetchXmlApiByServiceName" should {
    "return an Xml Api" in new Setup {
      whenGetXmlApiByServiceName(xmlApi1.serviceName, xmlApi1)

      val result: Option[XmlApi] = await(connector.fetchXmlApiByServiceName(xmlApi1.serviceName))

      result shouldBe Some(xmlApi1)
    }

    "handle exception correctly" in new Setup {

      whenGetXmlApiReturnsError(xmlApi1.serviceName, 500)

      intercept[UpstreamErrorResponse] {
        await(connector.fetchXmlApiByServiceName(xmlApi1.serviceName))
      }
    }
  }
}
