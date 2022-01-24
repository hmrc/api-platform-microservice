/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.apiplatformmicroservice.apidefinition.mocks

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatestplus.play.PlaySpec
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.{ApiCategoryDetails, ApiDefinition}
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.services.{ApiDefinitionService, PrincipalApiDefinitionService, SubordinateApiDefinitionService}

import scala.concurrent.Future

trait ApiDefinitionServiceModule extends PlaySpec with MockitoSugar with ArgumentMatchersSugar {

  trait ApiDefinitionServiceMock {
    def aMock: ApiDefinitionService

    object FetchAllApiDefinitions {
      def willReturn(apiDefinitions: ApiDefinition*) = {
        when(aMock.fetchAllApiDefinitions(*, *)).thenReturn(Future.successful(apiDefinitions.toList))
      }

      def willReturnNoApiDefinitions() = {
        when(aMock.fetchAllApiDefinitions(*, *)).thenReturn(Future.successful(List.empty))
      }

      def willThrowException(e: Exception) = {
        when(aMock.fetchAllApiDefinitions(*, *)).thenReturn(Future.failed(e))
      }
    }

    object FetchAllNonOpenAccessDefinitions {
      def willReturn(apiDefinitions: ApiDefinition*) = {
        when(aMock.fetchAllNonOpenAccessApiDefinitions(*, *)).thenReturn(Future.successful(apiDefinitions.toList))
      }

      def willReturnNoApiDefinitions() = {
        when(aMock.fetchAllNonOpenAccessApiDefinitions(*, *)).thenReturn(Future.successful(List.empty))
      }

      def willThrowException(e: Exception) = {
        when(aMock.fetchAllNonOpenAccessApiDefinitions(*, *)).thenReturn(Future.failed(e))
      }
    }

    object FetchApiDocumentationResource {
      def willReturnWsResponse(wsResponse: WSResponse) = {
        when(aMock.fetchApiDocumentationResource(*)(*, *)).thenReturn(Future.successful(Some(wsResponse)))
      }

      def willReturnNoResponse() = {
        when(aMock.fetchApiDocumentationResource(*)(*, *)).thenReturn(Future.successful(None))
      }

      def willThrowException(e: Exception) = {
        when(aMock.fetchApiDocumentationResource(*)(*, *)).thenReturn(Future.failed(e))
      }

      def verifyCalled(wantedNumberOfInvocations: Int) = {
        verify(aMock, times(wantedNumberOfInvocations)).fetchApiDocumentationResource(*)(*, *)
      }
    }

    object FetchDefinition {
      def willReturnApiDefinition(apiDefinition: ApiDefinition) = {
        when(aMock.fetchDefinition(*)(*, *)).thenReturn(Future.successful(Some(apiDefinition)))
      }

      def willReturnNoApiDefinition() = {
        when(aMock.fetchDefinition(*)(*, *)).thenReturn(Future.successful(None))
      }

      def willThrowException(e: Exception) = {
        when(aMock.fetchDefinition(*)(*, *)).thenReturn(Future.failed(e))
      }
    }

    object FetchApiCategoryDetails {
      def willReturnApiCategoryDetails(apiCategoryDetails: ApiCategoryDetails*) = {
        when(aMock.fetchAllApiCategoryDetails(*, *)).thenReturn(Future.successful(apiCategoryDetails.toList))
      }
    }
  }

  object SubordinateApiDefinitionServiceMock extends ApiDefinitionServiceMock {
    override val aMock = mock[SubordinateApiDefinitionService]
  }

  object PrincipalApiDefinitionServiceMock extends ApiDefinitionServiceMock {
    override val aMock = mock[PrincipalApiDefinitionService]
  }
}
