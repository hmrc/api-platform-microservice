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

package uk.gov.hmrc.apiplatformmicroservice.apidefinition.services

import scala.concurrent.ExecutionContext.Implicits.global

import akka.stream.testkit.NoMaterializer
import org.scalatest.Assertion

import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatformmicroservice.apidefinition.mocks.{ApiDefinitionServiceModule, ExtendedApiDefinitionForCollaboratorFetcherModule}
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.{ApiDefinitionTestDataHelper, ExtendedApiDefinitionExampleData, ResourceId}
import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ApiVersion

class ApiDocumentationResourceFetcherSpec extends AsyncHmrcSpec with ApiDefinitionTestDataHelper with ExtendedApiDefinitionExampleData {

  trait Setup extends ApiDefinitionServiceModule with ExtendedApiDefinitionForCollaboratorFetcherModule {
    implicit val headerCarrier = HeaderCarrier()
    implicit val mat           = NoMaterializer
    val serviceName            = "api-example-microservice"
    val resource               = "someResource"

    val resourceId    = ResourceId(serviceName, versionOne, resource)
    val noSuchVersion = resourceId.copy(version = ApiVersion("YouWontFindMe"))

    val mockWSResponse      = mock[WSResponse]
    when(mockWSResponse.status).thenReturn(OK)
    val mockErrorWSResponse = mock[WSResponse]
    when(mockErrorWSResponse.status).thenReturn(INTERNAL_SERVER_ERROR)

    def ensureResult: Assertion = {
      val oresult: Option[WSResponse] = await(underTest.fetch(resourceId))

      oresult mustBe 'defined
      oresult map (_.status) shouldEqual Some(OK)
      oresult mustBe Some(mockWSResponse)
    }

    def verifyNoEnvsCalled() = {
      PrincipalApiDefinitionServiceMock.FetchApiDocumentationResource.verifyCalled(0)
      SubordinateApiDefinitionServiceMock.FetchApiDocumentationResource.verifyCalled(0)
    }

    def verifyBothEnvsCalled() = {
      PrincipalApiDefinitionServiceMock.FetchApiDocumentationResource.verifyCalled(1)
      SubordinateApiDefinitionServiceMock.FetchApiDocumentationResource.verifyCalled(1)
    }

    def verifyOnlySubordinateEnvCalled() = {
      PrincipalApiDefinitionServiceMock.FetchApiDocumentationResource.verifyCalled(0)
      SubordinateApiDefinitionServiceMock.FetchApiDocumentationResource.verifyCalled(1)
    }

    def verifyOnlyPrincipalEnvCalled() = {
      PrincipalApiDefinitionServiceMock.FetchApiDocumentationResource.verifyCalled(1)
      SubordinateApiDefinitionServiceMock.FetchApiDocumentationResource.verifyCalled(0)
    }

    val underTest =
      new ApiDocumentationResourceFetcher(PrincipalApiDefinitionServiceMock.aMock, SubordinateApiDefinitionServiceMock.aMock, ExtendedApiDefinitionForCollaboratorFetcherMock.aMock)
  }

  "ApiDocumentationResourceFetcher" should {

    "return a resource from Subordinate when api and version exists" in new Setup {
      ExtendedApiDefinitionForCollaboratorFetcherMock.FetchCached.willReturnExtendedApiDefinition(anExtendedApiDefinitionWithPrincipalAndSubordinate)
      SubordinateApiDefinitionServiceMock.FetchApiDocumentationResource.willReturnWsResponse(mockWSResponse)

      val result = await(underTest.fetch(resourceId))

      result shouldBe 'defined

      verifyOnlySubordinateEnvCalled
    }

    "not attempt to fetch from subordinate when api version exists but is only present in principal environment" in new Setup {
      ExtendedApiDefinitionForCollaboratorFetcherMock.FetchCached.willReturnExtendedApiDefinition(anExtendedApiDefinitionWithOnlyPrincipal)
      PrincipalApiDefinitionServiceMock.FetchApiDocumentationResource.willReturnWsResponse(mockWSResponse)

      val result = await(underTest.fetch(resourceId))

      result shouldBe 'defined

      verifyOnlyPrincipalEnvCalled
    }

    "return nothing when api does not exist" in new Setup {
      ExtendedApiDefinitionForCollaboratorFetcherMock.FetchCached.willReturnNoExtendedApiDefinition()

      val result = await(underTest.fetch(resourceId))

      result shouldBe None

      verifyNoEnvsCalled
    }

    "return nothing when api exists but version does not exist" in new Setup {
      ExtendedApiDefinitionForCollaboratorFetcherMock.FetchCached.willReturnExtendedApiDefinition(anExtendedApiDefinitionWithPrincipalAndSubordinate)

      val result = await(underTest.fetch(noSuchVersion))

      result shouldBe None

      verifyNoEnvsCalled
    }

    "return the resource from principal when the subordinate fails" in new Setup {
      ExtendedApiDefinitionForCollaboratorFetcherMock.FetchCached.willReturnExtendedApiDefinition(anExtendedApiDefinitionWithPrincipalAndSubordinate)
      PrincipalApiDefinitionServiceMock.FetchApiDocumentationResource.willReturnWsResponse(mockWSResponse)
      SubordinateApiDefinitionServiceMock.FetchApiDocumentationResource.willReturnWsResponse(mockErrorWSResponse)

      val result = await(underTest.fetch(resourceId))

      result shouldBe 'defined

      verifyBothEnvsCalled
    }

    "return nothing when both environments return nothing" in new Setup {
      ExtendedApiDefinitionForCollaboratorFetcherMock.FetchCached.willReturnExtendedApiDefinition(anExtendedApiDefinitionWithPrincipalAndSubordinate)
      PrincipalApiDefinitionServiceMock.FetchApiDocumentationResource.willReturnNoResponse()
      SubordinateApiDefinitionServiceMock.FetchApiDocumentationResource.willReturnNoResponse()

      val result = await(underTest.fetch(resourceId))

      result shouldBe None

      verifyBothEnvsCalled
    }

    "will fail when extended api definition fetch fails" in new Setup {
      ExtendedApiDefinitionForCollaboratorFetcherMock.FetchCached.willThrowException(new RuntimeException("unexpected error"))

      private val ex = intercept[RuntimeException] {
        await(underTest.fetch(resourceId))
      }

      ex.getMessage mustBe "unexpected error"
    }

    "fail when both locations fail" in new Setup {
      ExtendedApiDefinitionForCollaboratorFetcherMock.FetchCached.willReturnExtendedApiDefinition(anExtendedApiDefinitionWithPrincipalAndSubordinate)
      PrincipalApiDefinitionServiceMock.FetchApiDocumentationResource.willReturnWsResponse(mockErrorWSResponse)
      SubordinateApiDefinitionServiceMock.FetchApiDocumentationResource.willReturnWsResponse(mockErrorWSResponse)

      await(underTest.fetch(resourceId)) shouldBe None
    }

    "fail with not found when principal returns nothing and it's not available in sandbox" in new Setup {
      ExtendedApiDefinitionForCollaboratorFetcherMock.FetchCached.willReturnExtendedApiDefinition(anExtendedApiDefinitionWithOnlyPrincipal)
      PrincipalApiDefinitionServiceMock.FetchApiDocumentationResource.willReturnNoResponse()
      SubordinateApiDefinitionServiceMock.FetchApiDocumentationResource.willReturnWsResponse(mockWSResponse)

      await(underTest.fetch(resourceId)) shouldBe None
    }
  }
}
