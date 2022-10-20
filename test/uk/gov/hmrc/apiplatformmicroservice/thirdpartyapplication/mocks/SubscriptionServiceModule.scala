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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.SubscriptionService

import scala.concurrent.Future.successful
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.SubscriptionService.CreateSubscriptionSuccess
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.SubscriptionService.CreateSubscriptionDenied
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.SubscriptionService.CreateSubscriptionDuplicate
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiIdentifier
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.SubscribeToApi

trait SubscriptionServiceModule extends MockitoSugar with ArgumentMatchersSugar {
  object SubscriptionServiceMock {
    val aMock = mock[SubscriptionService]

    object CreateSubscriptionForApplication {
      def willReturnSuccess = {
        when(aMock.createSubscriptionForApplication(*, *, *[ApiIdentifier], *)(*)).thenReturn(successful(CreateSubscriptionSuccess))
        when(aMock.createSubscriptionForApplication(*, *, *[SubscribeToApi], *)(*)).thenReturn(successful(CreateSubscriptionSuccess))
      }
      def willReturnDenied = {
        when(aMock.createSubscriptionForApplication(*, *, *[ApiIdentifier], *)(*)).thenReturn(successful(CreateSubscriptionDenied))
        when(aMock.createSubscriptionForApplication(*, *, *[SubscribeToApi], *)(*)).thenReturn(successful(CreateSubscriptionDenied))
      }
      def willReturnDuplicate = {
        when(aMock.createSubscriptionForApplication(*, *, *[ApiIdentifier], *)(*)).thenReturn(successful(CreateSubscriptionDuplicate))
        when(aMock.createSubscriptionForApplication(*, *, *[SubscribeToApi], *)(*)).thenReturn(successful(CreateSubscriptionDuplicate))
      }
    }

    object CreateManySubscriptionsForApplication {
      def willReturnOk = {
        when(aMock.createManySubscriptionsForApplication(*, *)(*)).thenReturn(successful(CreateSubscriptionSuccess))
      }

      def verifyCalled(apis: Set[ApiIdentifier]) = {
        verify(aMock).createManySubscriptionsForApplication(*, eqTo(apis))(*)
      }
    }
  }
}
