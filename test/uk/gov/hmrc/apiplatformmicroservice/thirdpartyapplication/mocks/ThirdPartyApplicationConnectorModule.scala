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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors._
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.models.APIIdentifier

import scala.concurrent.Future.{failed, successful}

trait ThirdPartyApplicationConnectorModule extends PlaySpec with MockitoSugar with ArgumentMatchersSugar {

  trait ThirdPartyApplicationConnectorMock {
    def aMock: ThirdPartyApplicationConnector

    object FetchApplicationsByEmail {
      def willReturnApplicationIds(applicationIds: String*) = {
        when(aMock.fetchApplicationsByEmail(*)(*)).thenReturn(successful(applicationIds))
      }

      def willThrowException(e: Exception) = {
        when(aMock.fetchApplicationsByEmail(*)(*)).thenReturn(failed(e))
      }
    }

    object FetchSubscriptionsByEmail {
      def willReturnSubscriptions(subscriptions: APIIdentifier*) = {
        when(aMock.fetchSubscriptionsByEmail(*)(*)).thenReturn(successful(subscriptions))
      }

      def willThrowException(e: Exception) = {
        when(aMock.fetchSubscriptionsByEmail(*)(*)).thenReturn(failed(e))
      }
    }
  }

  object SubordinateThirdPartyApplicationConnectorMock extends ThirdPartyApplicationConnectorMock {
    override val aMock = mock[SubordinateThirdPartyApplicationConnector]
  }

  object PrincipalThirdPartyApplicationConnectorMock extends ThirdPartyApplicationConnectorMock {
    override val aMock = mock[PrincipalThirdPartyApplicationConnector]
  }
}
