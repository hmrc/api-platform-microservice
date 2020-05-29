/*
 * Copyright 2020 HM Revenue & Customs
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
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.APIDefinition
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.services.{ApiDefinitionService, PrincipalApiDefinitionService, SubordinateApiDefinitionService}

import scala.concurrent.Future

trait ApiDefinitionServiceModule extends PlaySpec with MockitoSugar with ArgumentMatchersSugar {

  trait ApiDefinitionServiceMock {
    def aMock: ApiDefinitionService

    object FetchAllDefinitions {
      def willReturnApiDefinitions(apiDefinitions: APIDefinition*) = {
        when(aMock.fetchAllDefinitions(*, *)).thenReturn(Future.successful(apiDefinitions.toSeq))
      }

      def willReturnNoApiDefinitions() = {
        when(aMock.fetchAllDefinitions(*, *)).thenReturn(Future.successful(Seq.empty))
      }

      def willThrowException(e: Exception) = {
        when(aMock.fetchAllDefinitions(*, *)).thenReturn(Future.failed(e))
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
