package uk.gov.hrmc.apiplatformmicroservice.apiplatformmicroservice

import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}
import org.scalatest.OptionValues
import org.scalatestplus.play.WsScalaTestClient
import org.mockito.MockitoSugar
import org.mockito.ArgumentMatchersSugar
import play.api.test.DefaultAwaitTimeout
import play.api.test.FutureAwaits
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import uk.gov.hmrc.apiplatformmicroservice.subscriptionfields.ApiDefinitionMock
import uk.gov.hmrc.apiplatformmicroservice.subscriptionfields.ApplicationMock
import java.{util => ju}
import play.api.http.Status._
import play.api.http.HeaderNames._
import play.api.http.MimeTypes._
import uk.gov.hmrc.apiplatformmicroservice.utils._
import play.api.libs.ws.WSClient
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models._
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.controllers.ApiDefinitionController._
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.controllers.ApiDefinitionController

class ApiDefinitionSpec
    extends WordSpec
    with Matchers
    with OptionValues
    with WsScalaTestClient
    with MockitoSugar
    with ArgumentMatchersSugar
    with DefaultAwaitTimeout
    with FutureAwaits
    with BeforeAndAfterEach
    with GuiceOneServerPerSuite
    with ApiDefinitionMock
    with ApplicationMock
    with WiremockSetup
    with ConfigBuilder {

  override lazy val port = 8080

  lazy val baseUrl = s"http://localhost:$port"

  "WireMock" should {
    val wsClient = app.injector.instanceOf[WSClient]

    "stub get request" in {
      val applicationId = ApplicationId(ju.UUID.randomUUID.toString())

      mockFetchApplication(applicationId, Environment.PRODUCTION)
      mockFetchApiDefinition()

      val response = await(wsClient.url(s"$baseUrl/api-definitions")
        .withQueryStringParameters("applicationId" -> applicationId.value)
        .withHttpHeaders(ACCEPT -> JSON)
        .get())

      import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiDefinitionJsonFormatters._
      import play.api.libs.json._

      implicit val readsVersionData: Reads[ApiDefinitionController.VersionData] = Json.reads[VersionData]
      implicit val readsApiData: Reads[ApiDefinitionController.ApiData] = Json.reads[ApiData]

      response.status shouldBe OK
      val result: Map[ApiContext, ApiData] = Json.parse(response.body).validate[Map[ApiContext, ApiData]] match {
        case JsSuccess(v, _) => v
        case e: JsError      => fail(s"Bad response $e")
      }

      result should not be empty
    }
  }
}
