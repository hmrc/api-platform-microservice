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

import scala.concurrent.Future.successful

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.ClientId
import uk.gov.hmrc.apiplatform.modules.subscriptions.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.SubscriptionFieldsService

trait SubscriptionFieldsServiceModule {
  self: MockitoSugar with ArgumentMatchersSugar =>

  object SubscriptionFieldsServiceMock {
    val aMock = mock[SubscriptionFieldsService]

    object FetchFieldValuesWithDefaults {

      def willReturnFieldValues(subs: ApiFieldMap[FieldValue]) = {
        when(aMock.fetchFieldValuesWithDefaults(*, *[ClientId], *)(*)).thenReturn(successful(subs))
      }
    }

    object CreateFieldValues {

      def succeeds() =
        when(aMock.createFieldValues(*[ClientId], *, *)(*)).thenReturn(successful(Right(())))

      def fails() =
        when(aMock.createFieldValues(*[ClientId], *, *)(*)).thenReturn(successful(Left(Map.empty)))
    }
  }
}
