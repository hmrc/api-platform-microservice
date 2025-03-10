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

package uk.gov.hmrc.apiplatformmicroservice.apidefinition.mocks

import scala.concurrent.Future._

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatestplus.play.PlaySpec

import play.api.libs.json.JsValue
import uk.gov.hmrc.http.HttpResponse

import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApiVersionNbr
import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.DisplayApiEvent
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.services.{ApiDefinitionService, PrincipalApiDefinitionService, SubordinateApiDefinitionService}

trait ApiDefinitionServiceModule extends PlaySpec with MockitoSugar with ArgumentMatchersSugar {

  trait ApiDefinitionServiceMock {
    def aMock: ApiDefinitionService

    object FetchAllApiDefinitions {

      def willReturn(apiDefinitions: ApiDefinition*) = {
        when(aMock.fetchAllApiDefinitions(*, *)).thenReturn(successful(apiDefinitions.toList))
      }

      def willReturnNones() = {
        when(aMock.fetchAllApiDefinitions(*, *)).thenReturn(successful(List.empty))
      }

      def willThrowException(e: Exception) = {
        when(aMock.fetchAllApiDefinitions(*, *)).thenReturn(failed(e))
      }
    }

    object FetchAllNonOpenAccessDefinitions {

      def willReturn(apiDefinitions: ApiDefinition*) = {
        when(aMock.fetchAllNonOpenAccessApiDefinitions(*, *)).thenReturn(successful(apiDefinitions.toList))
      }

      def willReturnNones() = {
        when(aMock.fetchAllNonOpenAccessApiDefinitions(*, *)).thenReturn(successful(List.empty))
      }

      def willThrowException(e: Exception) = {
        when(aMock.fetchAllNonOpenAccessApiDefinitions(*, *)).thenReturn(failed(e))
      }
    }

    object FetchApiDocumentationResource {

      def willReturnWsResponse(wsResponse: HttpResponse) = {
        when(aMock.fetchApiDocumentationResource(*)(*, *)).thenReturn(successful(Some(wsResponse)))
      }

      def willReturnNoResponse() = {
        when(aMock.fetchApiDocumentationResource(*)(*, *)).thenReturn(successful(None))
      }

      def willThrowException(e: Exception) = {
        when(aMock.fetchApiDocumentationResource(*)(*, *)).thenReturn(failed(e))
      }

      def verifyCalled(wantedNumberOfInvocations: Int) = {
        verify(aMock, times(wantedNumberOfInvocations)).fetchApiDocumentationResource(*)(*, *)
      }
    }

    object FetchDefinition {

      def willReturn(apiDefinition: ApiDefinition) = {
        when(aMock.fetchDefinition(*[ServiceName])(*, *)).thenReturn(successful(Some(apiDefinition)))
      }

      def willReturnNone() = {
        when(aMock.fetchDefinition(*[ServiceName])(*, *)).thenReturn(successful(None))
      }

      def willThrowException(e: Exception) = {
        when(aMock.fetchDefinition(*[ServiceName])(*, *)).thenReturn(failed(e))
      }
    }

    object FetchApiSpecification {

      def willReturn(response: JsValue) = {
        when(aMock.fetchApiSpecification(*[ServiceName], *[ApiVersionNbr])(*, *)).thenReturn(successful(Some(response)))
      }

      def willReturnNone = {
        when(aMock.fetchApiSpecification(*[ServiceName], *[ApiVersionNbr])(*, *)).thenReturn(successful(None))
      }
    }

    object FetchApiEvents {

      def willReturn(displayApiEvents: List[DisplayApiEvent], includeNoChange: Boolean = true) = {
        when(aMock.fetchApiEvents(*[ServiceName], eqTo(includeNoChange))(*, *)).thenReturn(successful(displayApiEvents))
      }

      def willReturnEmptyList() = {
        when(aMock.fetchApiEvents(*[ServiceName], *[Boolean])(*, *)).thenReturn(successful(List.empty))
      }

      def willThrowException(e: Exception) = {
        when(aMock.fetchApiEvents(*[ServiceName], *[Boolean])(*, *)).thenReturn(failed(e))
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
