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

import scala.concurrent.Future

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.apiplatform.modules.applications.domain.models.ApplicationId
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.{Application, ApplicationWithSubscriptionData}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.ApplicationByIdFetcher
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ApiIdentifier

trait ApplicationByIdFetcherModule extends MockitoSugar with ArgumentMatchersSugar {

  object ApplicationByIdFetcherMock {
    val aMock = mock[ApplicationByIdFetcher]

    object FetchApplication {

      def willReturnApplication(app: Option[Application]) = {
        when(aMock.fetchApplication(*[ApplicationId])(*)).thenReturn(Future.successful(app))
      }

      def willThrowException(e: Exception) = {
        when(aMock.fetchApplication(*)(*)).thenReturn(Future.failed(e))
      }
    }

    object FetchApplicationWithSubscriptionData {

      def willReturnApplicationWithSubscriptionData(app: Application, subscriptions: Set[ApiIdentifier] = Set.empty) = {
        when(aMock.fetchApplicationWithSubscriptionData(*[ApplicationId])(*)).thenReturn(Future.successful(Some(ApplicationWithSubscriptionData(app, subscriptions))))
      }
    }
  }
}
