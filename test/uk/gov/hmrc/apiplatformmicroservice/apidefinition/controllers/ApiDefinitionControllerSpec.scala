/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.apiplatformmicroservice.apidefinition.controllers

import scala.concurrent.ExecutionContext.Implicits.global

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer

import play.api.libs.ws.{JsonBodyReadables, JsonBodyWritables}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.*
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ServiceName
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.mocks.ApiDefinitionServiceModule
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.{ApiDefinitionTestDataHelper, ResourceId}
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.services.*
import uk.gov.hmrc.apiplatformmicroservice.common.connectors.AuthConnector
import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.ApplicationByIdFetcher

class ApiDefinitionControllerSpec extends AsyncHmrcSpec with ApiDefinitionTestDataHelper with JsonBodyWritables with JsonBodyReadables {

  trait Setup extends ApiDefinitionServiceModule {
    implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
    implicit val actorSystem: ActorSystem     = ActorSystem("test") // NoMaterializer does not work with streamed responses

    val request                 = FakeRequest("GET", "/")
    val apiName                 = "hello-api"
    val serviceName             = ServiceName("hello-api")
    val version                 = ApiVersionNbr("1.0")
    val anApiDefinition         = apiDefinition(apiName)
    val anExtendedApiDefinition = extendedApiDefinition(apiName)
    val resource                = "application.yml"
    val resourceId              = ResourceId(serviceName, version, resource)

    val controller = new ApiDefinitionController(
      mock[ApplicationByIdFetcher],
      mock[ApiDefinitionsForApplicationFetcher],
      EnvironmentAwareApiDefinitionServiceMock.instance,
      mock[OpenAccessApisFetcher],
      mock[ApisFetcher],
      mock[AuthConnector.Config],
      mock[AuthConnector],
      stubControllerComponents(),
      mock[ApiIdentifiersForUpliftFetcher],
      mock[ApiEventsFetcher]
    )
  }

  "fetchApiDocumentationResource" should {
    "return a resource from production" in new Setup {
      val body = "some yaml"
      PrincipalApiDefinitionServiceMock.FetchApiDocumentationResource.willReturnWsResponseFor(resourceId, HttpResponse(OK, body))

      val result = controller.fetchApiDocumentationResource(Environment.Production, serviceName, version, resource)(request)

      status(result) shouldBe OK
      contentAsString(result) shouldBe body
    }

    "return a resource from sandbox" in new Setup {
      val body = "some yaml"
      SubordinateApiDefinitionServiceMock.FetchApiDocumentationResource.willReturnWsResponseFor(resourceId, HttpResponse(OK, body))

      val result = controller.fetchApiDocumentationResource(Environment.Sandbox, serviceName, version, resource)(request)

      status(result) shouldBe OK
      contentAsString(result) shouldBe body
    }
  }
}
