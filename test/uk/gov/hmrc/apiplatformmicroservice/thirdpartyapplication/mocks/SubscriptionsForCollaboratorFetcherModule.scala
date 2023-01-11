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

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiIdentifier
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.UserId

trait SubscriptionsForCollaboratorFetcherModule {
  self: MockitoSugar with ArgumentMatchersSugar =>

  import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.SubscriptionsForCollaboratorFetcher

  object SubscriptionsForCollaboratorFetcherMock {
    val aMock = mock[SubscriptionsForCollaboratorFetcher]

    def willReturnSubscriptions(subscriptions: ApiIdentifier*) = {
      when(aMock.fetch(*[UserId])(*)).thenReturn(successful(subscriptions.toSet))
    }

    def willThrowException(e: Exception) = {
      when(aMock.fetch(*[UserId])(*)).thenReturn(failed(e))
    }
  }

}
