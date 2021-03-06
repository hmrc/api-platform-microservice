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

import org.mockito.MockitoSugar
import org.mockito.ArgumentMatchersSugar
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.SubscriptionFieldsFetcher
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.ClientId
import scala.concurrent.Future.successful
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.FieldValue
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.subscriptions.SubscriptionFieldsDomain.ApiFieldMap

trait SubscriptionFieldsFetcherModule {
  self: MockitoSugar with ArgumentMatchersSugar =>
  
  object SubscriptionFieldsFetcherMock {
    val aMock = mock[SubscriptionFieldsFetcher]

    object fetchFieldValuesWithDefaults {
      def willReturnFieldValues(subs: ApiFieldMap[FieldValue]) = {
        when(aMock.fetchFieldValuesWithDefaults(*, *[ClientId], *)(*)).thenReturn(successful(subs))
      }
    }
  }
}
