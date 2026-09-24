package uk.gov.hmrc.apiplatformmicroservice.apidefinition.controllers

import uk.gov.hmrc.http.HeaderCarrier
import org.apache.pekko.stream.Materializer
import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiDefinitionTestDataHelper
import play.api.libs.ws.{JsonBodyWritables, JsonBodyReadables}
import org.apache.pekko.stream.testkit.NoMaterializer
import play.api.test.FakeRequest
import uk.gov.hmrc.apiplatform.modules.common.domain.models.*
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ServiceName
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.ApplicationByIdFetcher
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.services.*
import uk.gov.hmrc.apiplatformmicroservice.common.connectors.AuthConnector
import play.api.test.Helpers
import scala.concurrent.ExecutionContext.Implicits.global

class ApiDefinitionControllerSpec extends AsyncHmrcSpec with ApiDefinitionTestDataHelper with JsonBodyWritables with JsonBodyReadables {

  trait Setup {
    implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
    implicit val mat: Materializer            = NoMaterializer

    val request                 = FakeRequest("GET", "/")
    val apiName                 = "hello-api"
    val serviceName             = ServiceName("hello-api")
    val version                 = ApiVersionNbr("1.0")
    val anApiDefinition         = apiDefinition(apiName)
    val anExtendedApiDefinition = extendedApiDefinition(apiName)

    val controller = new ApiDefinitionController(
      mock[ApplicationByIdFetcher],
      mock[ApiDefinitionsForApplicationFetcher],
      mock[EnvironmentAwareApiDefinitionService],
      mock[OpenAccessApisFetcher],
      mock[ApisFetcher],
      mock[AuthConnector.Config],
      mock[AuthConnector],
      Helpers.stubControllerComponents(),
      mock[ApiIdentifiersForUpliftFetcher],
      mock[ApiEventsFetcher]
    )
    
    // val mockHttpResponse = mock[HttpResponse]
    // when(mockHttpResponse.status).thenReturn(OK)
    // when(mockHttpResponse.headers).thenReturn(
    //   Map(
    //     "Content-Length" -> Seq("500")
    //   )
    // )
    // when(mockHttpResponse.header(eqTo(PROXY_SAFE_CONTENT_TYPE))).thenReturn(None)
    // when(mockHttpResponse.header(eqTo(HeaderNames.CONTENT_TYPE))).thenReturn(Some("application/json"))
  }
}
