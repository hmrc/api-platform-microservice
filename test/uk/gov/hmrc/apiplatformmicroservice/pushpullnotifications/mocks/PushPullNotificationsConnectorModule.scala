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

package uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.mocks

import scala.concurrent.Future.successful

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.connectors.domain.BoxResponse
import uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.connectors.{EnvironmentAwarePushPullNotificationsConnector, PrincipalPushPullNotificationsConnector, PushPullNotificationsConnector, SubordinatePushPullNotificationsConnector}

trait PushPullNotificationsConnectorModule {
  self: MockitoSugar with ArgumentMatchersSugar =>

  abstract class PushPullNotificationsConnectorMock {
    val aMock: PushPullNotificationsConnector

    object FetchBoxes {

      def willReturnAllBoxes(boxes: List[BoxResponse]) = {
        when(aMock.fetchAllBoxes()((*))).thenReturn(successful(boxes))
      }
    }
  }

  object SubordinatePushPullNotificationsConnectorMock extends PushPullNotificationsConnectorMock {
    override val aMock: PushPullNotificationsConnector = mock[SubordinatePushPullNotificationsConnector](org.mockito.Mockito.withSettings().verboseLogging())
  }

  object PrincipalPushPullNotificationsConnectorMock extends PushPullNotificationsConnectorMock {
    override val aMock: PrincipalPushPullNotificationsConnector = mock[PrincipalPushPullNotificationsConnector](org.mockito.Mockito.withSettings().verboseLogging())
  }

  object EnvironmentAwarePushPullNotificationsConnectorMock {
    private val subordinateConnector = SubordinatePushPullNotificationsConnectorMock
    private val principalConnector   = PrincipalPushPullNotificationsConnectorMock

    lazy val instance = {
      new EnvironmentAwarePushPullNotificationsConnector(subordinateConnector.aMock, principalConnector.aMock)
    }

    lazy val Principal   = principalConnector
    lazy val Subordinate = subordinateConnector
  }
}
