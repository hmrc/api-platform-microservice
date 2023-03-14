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

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.domain.AddCollaboratorToTpaRequest
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.{EnvironmentAwareThirdPartyApplicationConnector, _}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.{Application, CreateApplicationRequestV2}
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.ApplicationId

trait ThirdPartyApplicationConnectorModule {
  self: MockitoSugar with ArgumentMatchersSugar =>

  abstract class ThirdPartyApplicationConnectorMock {
    val aMock: ThirdPartyApplicationConnector

    object FetchApplicationById {

      def willReturnApplication(application: Application) = {
        when(aMock.fetchApplication(*[ApplicationId])(*)).thenReturn(successful(Some(application)))
      }

      def willReturnNone = {
        when(aMock.fetchApplication(*[ApplicationId])(*)).thenReturn(successful(None))
      }

      def willThrowException(e: Exception) = {
        when(aMock.fetchApplication(*[ApplicationId])(*)).thenReturn(failed(e))
      }
    }

    object FetchApplicationsByUserId {

      def willReturnApplicationIds(applicationIds: ApplicationId*) = {
        when(aMock.fetchApplications(*[UserId])(*)).thenReturn(successful(applicationIds))
      }

      def willThrowException(e: Exception) = {
        when(aMock.fetchApplications(*[UserId])(*))
          .thenReturn(failed(e))
      }
    }

    object FetchSubscriptionsByUserId {

      def willReturnSubscriptions(subscriptions: ApiIdentifier*) = {
        when(aMock.fetchSubscriptions(*[UserId])(*)).thenReturn(successful(subscriptions))
      }

      def willThrowException(e: Exception) = {
        when(aMock.fetchSubscriptions(*[UserId])(*)).thenReturn(failed(e))
      }
    }

    object FetchSubscriptionsById {

      def willReturnSubscriptions(subscriptions: ApiIdentifier*) = {
        when(aMock.fetchSubscriptionsById(*[ApplicationId])(*)).thenReturn(successful(subscriptions.toSet[ApiIdentifier]))
      }

      def willThrowException(e: Exception) = {
        when(aMock.fetchSubscriptionsById(*[ApplicationId])(*)).thenReturn(failed(e))
      }
    }

    object SubscribeToApi {

      def willReturnSuccess = {
        when(aMock.subscribeToApi(*[ApplicationId], *)(*)).thenReturn(successful(SubscriptionUpdateSuccessResult))
      }
    }

    object AddCollaborator {

      def willReturnSuccess = {
        when(aMock.addCollaborator(*[ApplicationId], *)(*)).thenReturn(successful(AddCollaboratorSuccessResult(true)))
      }

      def willReturnFailure = {
        when(aMock.addCollaborator(*[ApplicationId], *)(*)).thenReturn(successful(CollaboratorAlreadyExistsFailureResult))
      }

      def verifyCalled(wantedNumberOfInvocations: Int, appId: ApplicationId, addCollaboratorToTpaRequest: AddCollaboratorToTpaRequest) = {
        verify(aMock, times(wantedNumberOfInvocations)).addCollaborator(eqTo(appId), eqTo(addCollaboratorToTpaRequest))(*)
      }
    }

    object UpdateApplication {

      def willReturnSuccess(application: Application) = {
        when(aMock.updateApplication(*[ApplicationId], *)(*)).thenReturn(successful(application))

      }

    }

    object CreateApplicationV2 {

      def willReturnSuccess(applcationId: ApplicationId) = {
        when(aMock.createApplicationV2(*)(*)).thenReturn(successful(applcationId))
      }

      def willThrowException(e: Exception) = {
        when(aMock.createApplicationV2(*)(*)).thenReturn(failed(e))
      }

      def verifyNotCalled() = {
        verify(aMock, never).createApplicationV2(*)(*)
      }

      def captureRequest() = {
        val capture = ArgCaptor[CreateApplicationRequestV2]
        verify(aMock).createApplicationV2(capture)(*)
        capture.value
      }
    }
  }

  object SubordinateThirdPartyApplicationConnectorMock extends ThirdPartyApplicationConnectorMock {
    override val aMock: ThirdPartyApplicationConnector = mock[SubordinateThirdPartyApplicationConnector]
  }

  object PrincipalThirdPartyApplicationConnectorMock extends ThirdPartyApplicationConnectorMock {

    object GetLinkedSubordinateApplicationId {
      def thenReturn(subordinateAppId: ApplicationId) = when(aMock.getLinkedSubordinateApplicationId(*[ApplicationId])(*)).thenReturn(successful(Some(subordinateAppId)))

      def thenReturnNothing = when(aMock.getLinkedSubordinateApplicationId(*[ApplicationId])(*)).thenReturn(successful(None))
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
