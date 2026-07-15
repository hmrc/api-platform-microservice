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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks

import scala.concurrent.Future.{failed, successful}

import org.mockito.captor.ArgCaptor
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.applications.core.interface.models.CreateApplicationRequestV2
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.{EnvironmentAwareThirdPartyApplicationConnector, *}

trait ThirdPartyApplicationConnectorModule {
  self: MockitoSugar & ArgumentMatchersSugar =>

  abstract class ThirdPartyApplicationConnectorMock {
    val aMock: ThirdPartyApplicationConnector

    object CreateApplicationV2 {

      def willReturnSuccess(applcationId: ApplicationId) = {
        when(aMock.createApplicationV2(*)(using *)).thenReturn(successful(applcationId))
      }

      def willThrowException(e: Exception) = {
        when(aMock.createApplicationV2(*)(using *)).thenReturn(failed(e))
      }

      def verifyNotCalled() = {
        verify(aMock, never).createApplicationV2(*)(using *)
      }

      def captureRequest() = {
        val capture = ArgCaptor[CreateApplicationRequestV2]
        verify(aMock).createApplicationV2(capture)(using *)
        capture.value
      }
    }
  }

  object SubordinateThirdPartyApplicationConnectorMock extends ThirdPartyApplicationConnectorMock {
    override val aMock: ThirdPartyApplicationConnector = mock[SubordinateThirdPartyApplicationConnector]
  }

  object PrincipalThirdPartyApplicationConnectorMock extends ThirdPartyApplicationConnectorMock {

    object GetLinkedSubordinateApplicationId {
      def thenReturn(subordinateAppId: ApplicationId) = when(aMock.getLinkedSubordinateApplicationId(*[ApplicationId])(using *)).thenReturn(successful(Some(subordinateAppId)))

      def thenReturnNothing = when(aMock.getLinkedSubordinateApplicationId(*[ApplicationId])(using *)).thenReturn(successful(None))
    }
    override val aMock: PrincipalThirdPartyApplicationConnector = mock[PrincipalThirdPartyApplicationConnector]
  }

  object EnvironmentAwareThirdPartyApplicationConnectorMock {
    private val subordinateConnector = SubordinateThirdPartyApplicationConnectorMock
    private val principalConnector   = PrincipalThirdPartyApplicationConnectorMock

    lazy val instance = {
      new EnvironmentAwareThirdPartyApplicationConnector(subordinateConnector.aMock, principalConnector.aMock)
    }

    lazy val Principal = principalConnector

    lazy val Subordinate = subordinateConnector
  }
}
