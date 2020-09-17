package uk.gov.hmrc.apiplatformmicroservice.apidefinition

import java.{util => ju}

import play.api.libs.json._
import play.api.http.HeaderNames._
import play.api.http.MimeTypes._
import play.api.http.Status._
import play.api.libs.ws.WSClient
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.controllers.ApiDefinitionController._
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models._
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.ApplicationMock
import uk.gov.hmrc.apiplatformmicroservice.subscriptionfields.SubscriptionFieldValuesMock
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.ClientId
import uk.gov.hmrc.apiplatformmicroservice.utils._

class ApiDefinitionSpec extends WireMockSpec with ApplicationMock with ApiDefinitionMock with SubscriptionFieldValuesMock {

  "WireMock" should {
    val wsClient = app.injector.instanceOf[WSClient]

    "stub get request for fetch api definitions" in {
      val applicationId = ApplicationId(ju.UUID.randomUUID.toString)
      val clientId = ClientId(ju.UUID.randomUUID.toString)

      mockFetchApplication(Environment.PRODUCTION, applicationId)
      mockFetchApplicationSubscriptions(Environment.PRODUCTION, applicationId)
      mockBulkFetchFieldValues(Environment.PRODUCTION, clientId)
      mockFetchApiDefinition(Environment.PRODUCTION)

      val response = await(wsClient.url(s"$baseUrl/api-definitions")
        .withQueryStringParameters("applicationId" -> applicationId.value)
        .withHttpHeaders(ACCEPT -> JSON)
        .get())

      import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiDefinitionJsonFormatters._

      implicit val readsVersionData: Reads[VersionData] = Json.reads[VersionData]
      implicit val readsApiData: Reads[ApiData] = Json.reads[ApiData]

      response.status shouldBe OK
      val result: Map[ApiContext, ApiData] = Json.parse(response.body).validate[Map[ApiContext, ApiData]] match {
        case JsSuccess(v, _) => v
        case e: JsError      => fail(s"Bad response $e")
      }

      result should not be empty
      withClue("No RETIRED status allowed: ") { result.values.flatMap(d => d.versions.values.map(v => v.status)).exists(s => s == APIStatus.RETIRED) shouldBe false }
      withClue("No Requires Trust allowed: ") { result.keys.exists(_ == ApiContext("trusted")) shouldBe false }

      val context = result(ApiContext("hello"))
      val versionKeys = context.versions.keys.toList

      versionKeys should contain(ApiVersion("3.0"))
      versionKeys should contain(ApiVersion("2.5rc"))
      versionKeys should contain(ApiVersion("2.0"))
      versionKeys should contain(ApiVersion("1.0"))

      versionKeys shouldNot contain(ApiVersion("4.0"))
    }

    "stub requests to fetch all API Category details" in {
      val category1 = APICategoryDetails("INCOME_TAX_MTD", "Income Tax (Making Tax Digital")
      val category2 = APICategoryDetails("AGENTS", "Agents")
      val category3 = APICategoryDetails("EXTRA_SANDBOX_CATEGORY", "Extra Sandbox Category")

      mockFetchAPICategoryDetails(Environment.SANDBOX, Seq(category1, category2, category3))
      mockFetchAPICategoryDetails(Environment.PRODUCTION, Seq(category1, category2))

      val response = await(wsClient.url(s"$baseUrl/api-categories")
        .withHttpHeaders(ACCEPT -> JSON)
        .get())

      import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiDefinitionJsonFormatters._

      response.status shouldBe OK
      val result: Seq[APICategoryDetails] = Json.parse(response.body).validate[Seq[APICategoryDetails]] match {
        case JsSuccess(v, _) => v
        case e: JsError      => fail(s"Bad response $e")
      }

      result.size should be (3)
      result should contain only (category1, category2, category3)
    }
  }
}
