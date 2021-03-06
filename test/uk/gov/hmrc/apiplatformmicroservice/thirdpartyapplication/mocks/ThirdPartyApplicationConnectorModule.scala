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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models._
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors._

import scala.concurrent.Future.{failed, successful}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.Application
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.EnvironmentAwareThirdPartyApplicationConnector
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.domain.AddCollaboratorToTpaRequest

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

    object FetchApplicationsByEmail {

      def willReturnApplicationIds(applicationIds: ApplicationId*) = {
        when(aMock.fetchApplications(*)(*)).thenReturn(successful(applicationIds))
      }

      def willThrowException(e: Exception) = {
        when(aMock.fetchApplications(*)(*))
          .thenReturn(failed(e))
      }
    }

    object FetchSubscriptionsByEmail {

      def willReturnSubscriptions(subscriptions: ApiIdentifier*) = {
        when(aMock.fetchSubscriptions(*)(*)).thenReturn(successful(subscriptions))
      }

      def willThrowException(e: Exception) = {
        when(aMock.fetchSubscriptions(*)(*)).thenReturn(failed(e))
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
  }

  object SubordinateThirdPartyApplicationConnectorMock extends ThirdPartyApplicationConnectorMock {
    override val aMock: ThirdPartyApplicationConnector = mock[SubordinateThirdPartyApplicationConnector]
  }

  object PrincipalThirdPartyApplicationConnectorMock extends ThirdPartyApplicationConnectorMock {
    override val aMock: ThirdPartyApplicationConnector = mock[PrincipalThirdPartyApplicationConnector]
  }

  object EnvironmentAwareThirdPartyApplicationConnectorMock {
    private val subordinateConnector = SubordinateThirdPartyApplicationConnectorMock
    private val principalConnector = PrincipalThirdPartyApplicationConnectorMock

    lazy val instance = {
      new EnvironmentAwareThirdPartyApplicationConnector(subordinateConnector.aMock, principalConnector.aMock)
    }

    lazy val Principal = principalConnector

    lazy val Subordinate = subordinateConnector
  }
}
