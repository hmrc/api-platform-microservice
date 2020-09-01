package uk.gov.hmrc.apiplatformmicroservice.apidefinition

import java.{util => ju}

import play.api.http.HeaderNames._
import play.api.http.MimeTypes._
import play.api.http.Status._
import play.api.libs.ws.WSClient
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.controllers.ApiDefinitionController
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.controllers.ApiDefinitionController._
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models._
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.ApplicationMock
import uk.gov.hmrc.apiplatformmicroservice.utils._

class ApiDefinitionSpec extends WireMockSpec with ApplicationMock with ApiDefinitionMock {

  "WireMock" should {
    val wsClient = app.injector.instanceOf[WSClient]

    "stub get request for fetch api definitions" in {
      val applicationId = ApplicationId(ju.UUID.randomUUID.toString)

      mockFetchApplication(Environment.PRODUCTION, applicationId)
      mockFetchApiDefinition(Environment.PRODUCTION)

      val response = await(wsClient.url(s"$baseUrl/api-definitions")
        .withQueryStringParameters("applicationId" -> applicationId.value)
        .withHttpHeaders(ACCEPT -> JSON)
        .get())

      import play.api.libs.json._
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
    }
  }
}
