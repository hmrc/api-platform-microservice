package uk.gov.hmrc.apiplatformmicroservice.combinedapis.controllers

import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.ApiDefinitionMock
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiCategory
import uk.gov.hmrc.apiplatformmicroservice.combinedapis.models.ApiType.{REST_API, XML_API}
import uk.gov.hmrc.apiplatformmicroservice.combinedapis.models.{BasicCombinedApiJsonFormatters, CombinedApi}
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.Environment.PRODUCTION
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.ApplicationMock
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.DeveloperIdentifier
import uk.gov.hmrc.apiplatformmicroservice.utils.WireMockSpec
import uk.gov.hmrc.apiplatformmicroservice.xmlapis.connectors.XmlApisMock
import uk.gov.hmrc.apiplatformmicroservice.xmlapis.models.XmlApi

class CombinedApisControllerISpec  extends WireMockSpec  with ApiDefinitionMock with ApplicationMock with XmlApisMock with BasicCombinedApiJsonFormatters {


  trait Setup {
    val wsClient = app.injector.instanceOf[WSClient]

    val xmlApi1: XmlApi = XmlApi(
      name = "xml api 1",
      context = "xml api context",
      description = "xml api description",
      categories = Some(List(ApiCategory("VAT")))
    )

    val xmlApi2: XmlApi = xmlApi1.copy(name = "xml api 2")
    val xmlApis = Seq(xmlApi1, xmlApi2)

    val developerId = DeveloperIdentifier("e8d1adb7-e211-4da2-89e8-2bf089a01833").get
  }

  "CombinedApisController" should {
    "return 200 with a combination of xml and rest apis when api definitions and xml services return results" in new Setup {

      mockFetchApiDefinition(PRODUCTION)
      whenGetAllXmlApis(xmlApis)
      mockFetchApplicationsForDeveloper(PRODUCTION, developerId)
      mockFetchSubscriptionsForDeveloper(PRODUCTION, developerId)

     val result =  await(wsClient.url(s"$baseUrl/combined-apis")
       .withQueryStringParameters("developerId" -> s"${developerId.asText}").get())

      result.status shouldBe 200
      val body = result.body
      body shouldBe "[{\"displayName\":\"Hello Another\",\"serviceName\":\"api-example-another\",\"categories\":[],\"apiType\":\"REST_API\"},{\"displayName\":\"Hello World\",\"serviceName\":\"api-example-microservice\",\"categories\":[],\"apiType\":\"REST_API\"},{\"displayName\":\"xml api 1\",\"serviceName\":\"xml api 1\",\"categories\":[{\"value\":\"VAT\"}],\"apiType\":\"XML_API\"},{\"displayName\":\"xml api 2\",\"serviceName\":\"xml api 2\",\"categories\":[{\"value\":\"VAT\"}],\"apiType\":\"XML_API\"}]"
      val apiList = Json.parse(body).as[List[CombinedApi]]
      apiList.count(_.apiType == XML_API) shouldBe 2
      apiList.count(_.apiType == REST_API) shouldBe 2
    }

    "return 500 when xml services returns Internal server error" in new Setup {

      mockFetchApiDefinition(PRODUCTION)
      mockFetchApplicationsForDeveloper(PRODUCTION, developerId)
      mockFetchSubscriptionsForDeveloper(PRODUCTION, developerId)
      whenGetAllXmlApisReturnsError(500)

      val result =  await(wsClient.url(s"$baseUrl/combined-apis")
        .withQueryStringParameters("developerId" -> s"${developerId.asText}").get())

      result.status shouldBe 500

    }

    "return 500 when api definition returns Internal server error" in new Setup {

      whenGetAllDefinitionsFails(PRODUCTION)(500)
      whenGetAllXmlApis(xmlApis)

      val result =  await(wsClient.url(s"$baseUrl/combined-apis/")
        .withQueryStringParameters("developerId" -> s"${developerId.asText}").get())


      result.status shouldBe 500

    }

    "return 500 when get applications as colloborator returns Not Found" in new Setup {

      mockFetchApiDefinition(PRODUCTION)
      mockFetchApplicationsForDeveloperNotFound(PRODUCTION, developerId)
      mockFetchSubscriptionsForDeveloper(PRODUCTION,developerId)

      val result =  await(wsClient.url(s"$baseUrl/combined-apis/")
        .withQueryStringParameters("developerId" -> s"${developerId.asText}").get())


      result.status shouldBe 500

    }

    "return 500 when get developer subscriptions returns Not Found" in new Setup {

      mockFetchApiDefinition(PRODUCTION)
      mockFetchApplicationsForDeveloper(PRODUCTION, developerId)
      mockFetchSubscriptionsForDeveloperNotFound(PRODUCTION,developerId)

      val result =  await(wsClient.url(s"$baseUrl/combined-apis/")
        .withQueryStringParameters("developerId" -> s"${developerId.asText}").get())


      result.status shouldBe 500

    }

    "return 500 when getcolloborators returns Not Found" in new Setup {

      whenGetAllDefinitionsFails(PRODUCTION)(500)
      mockFetchApplicationsForDeveloperNotFound(PRODUCTION,developerId)

      val result =  await(wsClient.url(s"$baseUrl/combined-apis/")
        .withQueryStringParameters("developerId" -> s"${developerId.asText}").get())


      result.status shouldBe 500

    }
  }
}