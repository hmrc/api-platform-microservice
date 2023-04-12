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

package uk.gov.hmrc.apiplatformmicroservice.combinedapis.controllers

import play.api.http.ContentTypes.JSON
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status._
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.libs.ws.WSClient
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.ApiDefinitionMock
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.{ApiCategory, ApiCategoryDetails}
import uk.gov.hmrc.apiplatformmicroservice.combinedapis.models.ApiType.{REST_API, XML_API}
import uk.gov.hmrc.apiplatformmicroservice.combinedapis.models.{BasicCombinedApiJsonFormatters, CombinedApi}
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.Environment
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.Environment.PRODUCTION
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.ApplicationMock
import uk.gov.hmrc.apiplatformmicroservice.utils.WireMockSpec
import uk.gov.hmrc.apiplatformmicroservice.xmlapis.connectors.XmlApisMock
import uk.gov.hmrc.apiplatformmicroservice.xmlapis.models.XmlApi
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiDefinitionJsonFormatters._
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiDefinitionTestDataHelper
import uk.gov.hmrc.apiplatform.modules.developers.domain.models.UserId

import java.util.UUID

class CombinedApisControllerISpec
    extends WireMockSpec
    with ApiDefinitionMock
    with ApplicationMock
    with XmlApisMock
    with BasicCombinedApiJsonFormatters
    with ApiDefinitionTestDataHelper {

  trait Setup {
    val wsClient = app.injector.instanceOf[WSClient]

    val xmlApi1: XmlApi = XmlApi(
      name = "xml api 1",
      serviceName = "xml-api-1",
      context = "xml api context",
      description = "xml api description",
      categories = Some(List(ApiCategory("VAT")))
    )

    val xmlApi2: XmlApi = xmlApi1.copy(name = "xml api 2")
    val xmlApis         = Seq(xmlApi1, xmlApi2)

    val userId = UserId(UUID.fromString("e8d1adb7-e211-4da2-89e8-2bf089a01833"))

    val apiDefinition1    = apiDefinition(name = "service1").copy(categories = List(ApiCategory("OTHER"), ApiCategory("INCOME_TAX_MTD")))
    val apiDefinition2    = apiDefinition(name = "service2").copy(categories = List(ApiCategory("VAT"), ApiCategory("OTHER")))
    val listOfDefinitions = List(apiDefinition1, apiDefinition2)
  }

  "CombinedApisController" should {
    "return OK with a combination of xml and rest apis when api definitions and xml services return results" in new Setup {

      mockFetchApplicationsForDeveloper(PRODUCTION, userId)
      mockFetchSubscriptionsForDeveloper(PRODUCTION, userId)
      mockFetchApiDefinition(PRODUCTION)
      whenGetAllXmlApis(xmlApis: _*)

      val result = await(wsClient.url(s"$baseUrl/combined-rest-xml-apis/developer")
        .withQueryStringParameters("developerId" -> s"${userId.value}").get())

      result.status shouldBe OK
      val body    = result.body
      body shouldBe """[{"displayName":"Hello Another","serviceName":"api-example-another","categories":[],"apiType":"REST_API","accessType":"PUBLIC"},{"displayName":"Hello World","serviceName":"api-example-microservice","categories":[],"apiType":"REST_API","accessType":"PUBLIC"},{"displayName":"xml api 1","serviceName":"xml-api-1","categories":[{"value":"VAT"}],"apiType":"XML_API","accessType":"PUBLIC"},{"displayName":"xml api 2","serviceName":"xml-api-1","categories":[{"value":"VAT"}],"apiType":"XML_API","accessType":"PUBLIC"}]"""
      val apiList = Json.parse(body).as[List[CombinedApi]]
      apiList.count(_.apiType == XML_API) shouldBe 2
      apiList.count(_.apiType == REST_API) shouldBe 2
    }

    "return INTERNAL_SERVER_ERROR when xml services returns Internal server error" in new Setup {

      mockFetchApiDefinition(PRODUCTION)
      mockFetchApplicationsForDeveloper(PRODUCTION, userId)
      mockFetchSubscriptionsForDeveloper(PRODUCTION, userId)
      whenGetAllXmlApisReturnsError(INTERNAL_SERVER_ERROR)

      val result = await(wsClient.url(s"$baseUrl/combined-rest-xml-apis/developer")
        .withQueryStringParameters("developerId" -> s"${userId.value}").get())

      result.status shouldBe INTERNAL_SERVER_ERROR

    }

    "return INTERNAL_SERVER_ERROR when api definition returns Internal server error" in new Setup {

      whenGetAllDefinitionsFails(PRODUCTION)(INTERNAL_SERVER_ERROR)
      whenGetAllXmlApis(xmlApis: _*)

      val result = await(wsClient.url(s"$baseUrl/combined-rest-xml-apis/developer")
        .withQueryStringParameters("developerId" -> s"${userId.value}").get())

      result.status shouldBe INTERNAL_SERVER_ERROR

    }

    "return INTERNAL_SERVER_ERROR when get applications as collaborator returns Not Found" in new Setup {

      mockFetchApiDefinition(PRODUCTION)
      mockFetchApplicationsForDeveloperNotFound(PRODUCTION, userId)
      mockFetchSubscriptionsForDeveloper(PRODUCTION, userId)

      val result = await(wsClient.url(s"$baseUrl/combined-rest-xml-apis/developer")
        .withQueryStringParameters("developerId" -> s"${userId.value}").get())

      result.status shouldBe INTERNAL_SERVER_ERROR

    }

    "return INTERNAL_SERVER_ERROR when get developer subscriptions returns Not Found" in new Setup {

      mockFetchApiDefinition(PRODUCTION)
      mockFetchApplicationsForDeveloper(PRODUCTION, userId)
      mockFetchSubscriptionsForDeveloperNotFound(PRODUCTION, userId)

      val result = await(wsClient.url(s"$baseUrl/combined-rest-xml-apis/developer")
        .withQueryStringParameters("developerId" -> s"${userId.value}").get())

      result.status shouldBe INTERNAL_SERVER_ERROR

    }

    "return INTERNAL_SERVER_ERROR when getcolloborators returns Not Found" in new Setup {

      whenGetAllDefinitionsFails(PRODUCTION)(INTERNAL_SERVER_ERROR)
      mockFetchApplicationsForDeveloperNotFound(PRODUCTION, userId)

      val result = await(wsClient.url(s"$baseUrl/combined-rest-xml-apis/developer")
        .withQueryStringParameters("developerId" -> s"${userId.value}").get())

      result.status shouldBe INTERNAL_SERVER_ERROR

    }

    "stub requests to fetch all API Category details" in new Setup {
      val category1   = ApiCategoryDetails("INCOME_TAX_MTD", "Income Tax (Making Tax Digital")
      val category2   = ApiCategoryDetails("AGENTS", "Agents")
      val category3   = ApiCategoryDetails("EXTRA_SANDBOX_CATEGORY", "Extra Sandbox Category")
      val xmlCategory = ApiCategoryDetails("VAT", "VAT")

      whenGetAllXmlApis(xmlApis: _*)
      mockFetchApiCategoryDetails(Environment.SANDBOX, Seq(category1, category2, category3))
      mockFetchApiCategoryDetails(Environment.PRODUCTION, Seq(category1, category2))

      val response = await(wsClient.url(s"$baseUrl/api-categories/combined")
        .withHttpHeaders(ACCEPT -> JSON)
        .get())

      response.status shouldBe OK
      val result: Seq[ApiCategoryDetails] = Json.parse(response.body).validate[Seq[ApiCategoryDetails]] match {
        case JsSuccess(v, _) => v
        case e: JsError      => fail(s"Bad response $e")
      }

      result.size should be(5)
      result should contain only (category1, category2, category3, xmlCategory)
    }

  }

  "fetchAllApis" should {
    "return combined apis when both xml and rest apis are returned" in new Setup {
      whenGetAllXmlApis(xmlApis: _*)
      whenGetAllDefinitions(Environment.SANDBOX)(apiDefinition1)
      whenGetAllDefinitions(Environment.PRODUCTION)(apiDefinition2)

      val result = await(wsClient.url(s"$baseUrl/combined-rest-xml-apis/")
        .withHttpHeaders(ACCEPT -> JSON)
        .get())

      result.status shouldBe OK
      val body    = result.body
      body shouldBe """[{"displayName":"service1","serviceName":"service1","categories":[{"value":"OTHER"},{"value":"INCOME_TAX_MTD"}],"apiType":"REST_API","accessType":"PUBLIC"},{"displayName":"service2","serviceName":"service2","categories":[{"value":"VAT"},{"value":"OTHER"}],"apiType":"REST_API","accessType":"PUBLIC"},{"displayName":"xml api 1","serviceName":"xml-api-1","categories":[{"value":"VAT"}],"apiType":"XML_API","accessType":"PUBLIC"},{"displayName":"xml api 2","serviceName":"xml-api-1","categories":[{"value":"VAT"}],"apiType":"XML_API","accessType":"PUBLIC"}]"""
      val apiList = Json.parse(body).as[List[CombinedApi]]
      apiList.count(_.apiType == XML_API) shouldBe 2
      apiList.count(_.apiType == REST_API) shouldBe 2
    }

    "return combined apis when only xml apis are returned" in new Setup {
      whenGetAllXmlApis(xmlApis: _*)
      whenGetAllDefinitionsFindsNothing(Environment.SANDBOX)
      whenGetAllDefinitionsFindsNothing(Environment.PRODUCTION)

      val result = await(wsClient.url(s"$baseUrl/combined-rest-xml-apis/")
        .withHttpHeaders(ACCEPT -> JSON)
        .get())

      result.status shouldBe OK
      val body    = result.body
      body shouldBe """[{"displayName":"xml api 1","serviceName":"xml-api-1","categories":[{"value":"VAT"}],"apiType":"XML_API","accessType":"PUBLIC"},{"displayName":"xml api 2","serviceName":"xml-api-1","categories":[{"value":"VAT"}],"apiType":"XML_API","accessType":"PUBLIC"}]"""
      val apiList = Json.parse(body).as[List[CombinedApi]]
      apiList.count(_.apiType == XML_API) shouldBe 2
      apiList.count(_.apiType == REST_API) shouldBe 0
    }

    "return combined apis when only rest apis are returned" in new Setup {
      whenGetAllXmlApis(List.empty: _*)
      whenGetAllDefinitions(Environment.SANDBOX)(apiDefinition1)
      whenGetAllDefinitions(Environment.PRODUCTION)(apiDefinition2)

      val result = await(wsClient.url(s"$baseUrl/combined-rest-xml-apis/")
        .withHttpHeaders(ACCEPT -> JSON)
        .get())

      result.status shouldBe OK
      val body    = result.body
      body shouldBe """[{"displayName":"service1","serviceName":"service1","categories":[{"value":"OTHER"},{"value":"INCOME_TAX_MTD"}],"apiType":"REST_API","accessType":"PUBLIC"},{"displayName":"service2","serviceName":"service2","categories":[{"value":"VAT"},{"value":"OTHER"}],"apiType":"REST_API","accessType":"PUBLIC"}]"""
      val apiList = Json.parse(body).as[List[CombinedApi]]
      apiList.count(_.apiType == XML_API) shouldBe 0
      apiList.count(_.apiType == REST_API) shouldBe 2
    }

    "return no apis when no xml or rest apis are returned" in new Setup {
      whenGetAllXmlApis(List.empty: _*)
      whenGetAllDefinitionsFindsNothing(Environment.SANDBOX)
      whenGetAllDefinitionsFindsNothing(Environment.PRODUCTION)

      val result = await(wsClient.url(s"$baseUrl/combined-rest-xml-apis/")
        .withHttpHeaders(ACCEPT -> JSON)
        .get())

      result.status shouldBe OK
      val body = result.body
      body shouldBe """[]"""

    }

    "return error when no xml returns error" in new Setup {
      whenGetAllXmlApisReturnsError(INTERNAL_SERVER_ERROR)
      whenGetAllDefinitionsFindsNothing(Environment.SANDBOX)
      whenGetAllDefinitionsFindsNothing(Environment.PRODUCTION)

      val result = await(wsClient.url(s"$baseUrl/combined-rest-xml-apis/")
        .withHttpHeaders(ACCEPT -> JSON)
        .get())

      result.status shouldBe INTERNAL_SERVER_ERROR
      val body = result.body
      body shouldBe """{"code":"UNKNOWN_ERROR","message":"An unexpected error occurred"}"""

    }

    "return a rest definition when sandbox definitions returns error" in new Setup {
      whenGetAllXmlApis(List.empty: _*)
      whenGetAllDefinitionsFails(Environment.SANDBOX)(INTERNAL_SERVER_ERROR)
      whenGetAllDefinitions(Environment.PRODUCTION)(apiDefinition2)

      val result = await(wsClient.url(s"$baseUrl/combined-rest-xml-apis/")
        .withHttpHeaders(ACCEPT -> JSON)
        .get())

      result.status shouldBe OK
      val body    = result.body
      body shouldBe """[{"displayName":"service2","serviceName":"service2","categories":[{"value":"VAT"},{"value":"OTHER"}],"apiType":"REST_API","accessType":"PUBLIC"}]"""
      val apiList = Json.parse(body).as[List[CombinedApi]]
      apiList.count(_.apiType == XML_API) shouldBe 0
      apiList.count(_.apiType == REST_API) shouldBe 1

    }

    "return error when production definition returns error" in new Setup {
      whenGetAllXmlApis(List.empty: _*)
      whenGetAllDefinitions(Environment.SANDBOX)(apiDefinition2)
      whenGetAllDefinitionsFails(Environment.PRODUCTION)(INTERNAL_SERVER_ERROR)

      val result = await(wsClient.url(s"$baseUrl/combined-rest-xml-apis/")
        .withHttpHeaders(ACCEPT -> JSON)
        .get())

      result.status shouldBe INTERNAL_SERVER_ERROR
      val body = result.body
      body shouldBe """{"code":"UNKNOWN_ERROR","message":"An unexpected error occurred"}"""

    }

    "return error when xml api fetcher and production definition returns error" in new Setup {
      whenGetAllXmlApisReturnsError(INTERNAL_SERVER_ERROR)
      whenGetAllDefinitions(Environment.SANDBOX)(apiDefinition2)
      whenGetAllDefinitionsFails(Environment.PRODUCTION)(INTERNAL_SERVER_ERROR)

      val result = await(wsClient.url(s"$baseUrl/combined-rest-xml-apis/")
        .withHttpHeaders(ACCEPT -> JSON)
        .get())

      result.status shouldBe INTERNAL_SERVER_ERROR
      val body = result.body
      body shouldBe """{"code":"UNKNOWN_ERROR","message":"An unexpected error occurred"}"""

    }
  }
}
