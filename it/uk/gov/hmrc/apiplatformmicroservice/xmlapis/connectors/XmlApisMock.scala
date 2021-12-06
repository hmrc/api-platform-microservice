package uk.gov.hmrc.apiplatformmicroservice.xmlapis.connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.Status.OK
import play.utils.UriEncoding
import uk.gov.hmrc.apiplatformmicroservice.common.utils.WireMockSugarExtensions
import uk.gov.hmrc.apiplatformmicroservice.utils.WireMockSpec
import uk.gov.hmrc.apiplatformmicroservice.xmlapis.models.{BasicXmlApisJsonFormatters, XmlApi}

trait XmlApisMock extends WireMockSpec with BasicXmlApisJsonFormatters with WireMockSugarExtensions {

  val getAllXmlApisUrl = "/api-platform-xml-services/xml/apis"

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
  def whenGetAllXmlApisReturnsError(status: Int): Unit ={
    stubForProd(
      get(urlEqualTo(s"$getAllXmlApisUrl"))
        .willReturn(
          aResponse()
            .withStatus(status)
        )
    )
  }


  def whenGetXmlApiByServiceName(name: String, xmlApi: XmlApi): Unit ={
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
  def whenGetXmlApiReturnsError(name: String, status: Int): Unit ={
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
