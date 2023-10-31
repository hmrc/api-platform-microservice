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

import scala.concurrent.Future

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatestplus.play.PlaySpec

import uk.gov.hmrc.apiplatform.modules.apis.domain.models.{ExtendedApiDefinition, ServiceName}
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.services.ExtendedApiDefinitionForCollaboratorFetcher

trait ExtendedApiDefinitionForCollaboratorFetcherModule extends PlaySpec with MockitoSugar with ArgumentMatchersSugar {

  object ExtendedApiDefinitionForCollaboratorFetcherMock {
    val aMock = mock[ExtendedApiDefinitionForCollaboratorFetcher]

    object Fetch {

      def willReturnExtendedApiDefinition(extendedApi: ExtendedApiDefinition) = {
        when(aMock.fetch(*[ServiceName], *)(*)).thenReturn(Future.successful(Some(extendedApi)))
      }

      def willReturnNoExtendedApiDefinition() = {
        when(aMock.fetch(*[ServiceName], *)(*)).thenReturn(Future.successful(None))
      }

      def willThrowException(e: Exception) = {
        when(aMock.fetch(*[ServiceName], *)(*)).thenReturn(Future.failed(e))
      }
    }

    object FetchCached {

      def willReturnExtendedApiDefinition(extendedApi: ExtendedApiDefinition) = {
        when(aMock.fetchCached(*[ServiceName], *)(*)).thenReturn(Future.successful(Some(extendedApi)))
      }

      def willReturnNoExtendedApiDefinition() = {
        when(aMock.fetchCached(*[ServiceName], *)(*)).thenReturn(Future.successful(None))
      }

      def willThrowException(e: Exception) = {
        when(aMock.fetchCached(*[ServiceName], *)(*)).thenReturn(Future.failed(e))
      }
    }
  }

}
