/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.apiplatformmicroservice.apidefinition.services

import org.scalatest.Assertion
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.mocks.{ApiDefinitionServiceModule, ExtendedApiDefinitionForCollaboratorFetcherModule}
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiStatus.STABLE
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.{ApiAvailability, ApiDefinitionTestDataHelper, ApiVersion, PublicApiAccess, ResourceId}
import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}

import scala.concurrent.ExecutionContext.Implicits.global
import akka.stream.testkit.NoMaterializer
import org.mockito.MockitoSugar
import org.mockito.ArgumentMatchersSugar

class ApiDocumentationResourceFetcherSpec extends AsyncHmrcSpec with ApiDefinitionTestDataHelper {

  trait Setup extends ApiDefinitionServiceModule with ExtendedApiDefinitionForCollaboratorFetcherModule with MockitoSugar with ArgumentMatchersSugar {
    implicit val headerCarrier = HeaderCarrier()
    implicit val mat = NoMaterializer
    val serviceName = "api-example-microservice"
    val versionOne = ApiVersion("1.0")
    val resource = "someResource"
    val resourceId = ResourceId(serviceName, versionOne, resource)
    val mockWSResponse = mock[WSResponse]
    when(mockWSResponse.status).thenReturn(OK)
    val mockErrorWSResponse = mock[WSResponse]
    when(mockErrorWSResponse.status).thenReturn(INTERNAL_SERVER_ERROR)
    val apiName = "hello-api"

    val anExtendedApiDefinitionWithOnlySubordinate = extendedApiDefinition(
      apiName,
      List(extendedApiVersion(versionOne, STABLE, None, Some(ApiAvailability(endpointsEnabled = true, PublicApiAccess(), loggedIn = true, authorised = true))))
    )

    val anExtendedApiDefinitionWithOnlyPrincipal = extendedApiDefinition(
      apiName,
      List(extendedApiVersion(versionOne, STABLE, Some(ApiAvailability(endpointsEnabled = true, PublicApiAccess(), loggedIn = true, authorised = true)), None))
    )

    val anExtendedApiDefinitionWithPrincipalAndSubordinate = extendedApiDefinition(
      apiName,
      List(extendedApiVersion(
        versionOne,
        STABLE,
        Some(ApiAvailability(endpointsEnabled = true, PublicApiAccess(), loggedIn = true, authorised = true)),
        Some(ApiAvailability(endpointsEnabled = true, PublicApiAccess(), loggedIn = true, authorised = true))
      ))
    )

    def ensureResult: Assertion = {
      val oresult: Option[WSResponse] = await(underTest.fetch(resourceId))

      oresult shouldBe 'defined
      oresult map (_.status) shouldEqual Some(OK)
      oresult shouldBe Some(mockWSResponse)
    }

    val underTest =
      new ApiDocumentationResourceFetcher(PrincipalApiDefinitionServiceMock.aMock, SubordinateApiDefinitionServiceMock.aMock, ExtendedApiDefinitionForCollaboratorFetcherMock.aMock)
  }

  "ApiDocumentationResourceFetcher" should {
    "will not invoke subordinate resource search if the subordinate is disabled" in new Setup {
      ExtendedApiDefinitionForCollaboratorFetcherMock.willReturnExtendedApiDefinition(anExtendedApiDefinitionWithOnlyPrincipal)
      PrincipalApiDefinitionServiceMock.FetchApiDocumentationResource.willReturnWsResponse(mockWSResponse)

      ensureResult
      PrincipalApiDefinitionServiceMock.FetchApiDocumentationResource.verifyCalled(1)
      SubordinateApiDefinitionServiceMock.FetchApiDocumentationResource.verifyCalled(0)
    }

    "will fail when extended api definition fetch fails" in new Setup {
      ExtendedApiDefinitionForCollaboratorFetcherMock.willThrowException(new RuntimeException("unexpected error"))

      private val ex = intercept[RuntimeException] {
        await(underTest.fetch(resourceId))
      }

      ex.getMessage shouldBe "unexpected error"
    }

    "will fail with not found when no apis available" in new Setup {
      ExtendedApiDefinitionForCollaboratorFetcherMock.willReturnNoExtendedApiDefinition()

      private val ex = intercept[IllegalArgumentException] {
        await(underTest.fetch(resourceId))
      }

      ex.getMessage shouldBe "Version 1.0 of api-example-microservice not found"
    }

    "return the resource fetched from the subordinate when the version exists in both locations" in new Setup {
      ExtendedApiDefinitionForCollaboratorFetcherMock.willReturnExtendedApiDefinition(anExtendedApiDefinitionWithPrincipalAndSubordinate)
      PrincipalApiDefinitionServiceMock.FetchApiDocumentationResource.willReturnWsResponse(mockWSResponse)
      SubordinateApiDefinitionServiceMock.FetchApiDocumentationResource.willReturnWsResponse(mockWSResponse)

      ensureResult

      PrincipalApiDefinitionServiceMock.FetchApiDocumentationResource.verifyCalled(0)
      SubordinateApiDefinitionServiceMock.FetchApiDocumentationResource.verifyCalled(1)
    }

    "return the resource from principal when the subordinate fails" in new Setup {
      ExtendedApiDefinitionForCollaboratorFetcherMock.willReturnExtendedApiDefinition(anExtendedApiDefinitionWithPrincipalAndSubordinate)
      PrincipalApiDefinitionServiceMock.FetchApiDocumentationResource.willReturnWsResponse(mockWSResponse)
      SubordinateApiDefinitionServiceMock.FetchApiDocumentationResource.willReturnWsResponse(mockErrorWSResponse)

      ensureResult
    }

    "return the resource when the principal would have failed but we already got a response from subordinate" in new Setup {
      ExtendedApiDefinitionForCollaboratorFetcherMock.willReturnExtendedApiDefinition(anExtendedApiDefinitionWithPrincipalAndSubordinate)
      PrincipalApiDefinitionServiceMock.FetchApiDocumentationResource.willReturnWsResponse(mockErrorWSResponse)
      SubordinateApiDefinitionServiceMock.FetchApiDocumentationResource.willReturnWsResponse(mockWSResponse)

      ensureResult
    }

    "fail when both locations fail" in new Setup {
      ExtendedApiDefinitionForCollaboratorFetcherMock.willReturnExtendedApiDefinition(anExtendedApiDefinitionWithPrincipalAndSubordinate)
      PrincipalApiDefinitionServiceMock.FetchApiDocumentationResource.willReturnWsResponse(mockErrorWSResponse)
      SubordinateApiDefinitionServiceMock.FetchApiDocumentationResource.willReturnWsResponse(mockErrorWSResponse)

      private val ex = intercept[NotFoundException] {
        await(underTest.fetch(resourceId))
      }

      ex.getMessage shouldBe "someResource not found for api-example-microservice 1.0"
    }

    "fail with not found when both locations return nothing" in new Setup {
      ExtendedApiDefinitionForCollaboratorFetcherMock.willReturnExtendedApiDefinition(anExtendedApiDefinitionWithPrincipalAndSubordinate)
      PrincipalApiDefinitionServiceMock.FetchApiDocumentationResource.willReturnNoResponse()
      SubordinateApiDefinitionServiceMock.FetchApiDocumentationResource.willReturnNoResponse()

      private val ex = intercept[NotFoundException] {
        await(underTest.fetch(resourceId))
      }

      ex.getMessage shouldBe "someResource not found for api-example-microservice 1.0"
    }

    "fail with not found when principal returns nothing and it's not available in sandbox" in new Setup {
      ExtendedApiDefinitionForCollaboratorFetcherMock.willReturnExtendedApiDefinition(anExtendedApiDefinitionWithOnlyPrincipal)
      PrincipalApiDefinitionServiceMock.FetchApiDocumentationResource.willReturnNoResponse()
      SubordinateApiDefinitionServiceMock.FetchApiDocumentationResource.willReturnWsResponse(mockWSResponse)

      private val ex = intercept[NotFoundException] {
        await(underTest.fetch(resourceId))
      }

      ex.getMessage shouldBe "someResource not found for api-example-microservice 1.0"
    }
  }
}
