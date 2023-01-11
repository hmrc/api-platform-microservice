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

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.Status.OK
import play.utils.UriEncoding
import uk.gov.hmrc.apiplatformmicroservice.common.utils.WireMockSugarExtensions
import uk.gov.hmrc.apiplatformmicroservice.utils.WireMockSpec
import uk.gov.hmrc.apiplatformmicroservice.xmlapis.models.{BasicXmlApisJsonFormatters, XmlApi}

trait XmlApisMock extends WireMockSpec with BasicXmlApisJsonFormatters with WireMockSugarExtensions {

  val getAllXmlApisUrl = "/api-platform-xml-services/xml/apis"

  def getXmlApiUrl(name: String) = s"/api-platform-xml-services/xml/api/${UriEncoding.encodePathSegment(name, "UTF-8")}"

  val getXmlApiUrl = "/api-platform-xml-services/xml/api"

  def whenGetAllXmlApis(xmlApis: XmlApi*): Unit = {
    stubForProd(
      get(urlEqualTo(s"$getAllXmlApisUrl"))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withJsonBody(xmlApis.toList)
        )
    )
  }

  def whenGetAllXmlApisReturnsError(status: Int): Unit = {
    stubForProd(
      get(urlEqualTo(s"$getAllXmlApisUrl"))
        .willReturn(
          aResponse()
            .withStatus(status)
        )
    )
  }

  def whenGetXmlApiByName(name: String, xmlApi: XmlApi): Unit = {
    stubForProd(
      get(urlEqualTo(s"${getXmlApiUrl(name)}"))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withJsonBody(xmlApi)
        )
    )
  }

  def whenGetXmlApiByServiceName(name: String, xmlApi: XmlApi): Unit = {
    stubForProd(
      get(urlPathEqualTo(s"$getXmlApiUrl"))
        .withQueryParam("serviceName", equalTo(name))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withJsonBody(xmlApi)
        )
    )
  }

  def whenGetXmlApiReturnsError(name: String, status: Int): Unit = {
    stubForProd(
      get(urlPathEqualTo(s"$getXmlApiUrl"))
        .withQueryParam("serviceName", equalTo(name))
        .willReturn(
          aResponse()
            .withStatus(status)
        )
    )
  }

}
