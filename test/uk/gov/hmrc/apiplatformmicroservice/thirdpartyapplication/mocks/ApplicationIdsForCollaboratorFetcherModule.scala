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
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.ApplicationIdsForCollaboratorFetcher

import scala.concurrent.Future

trait ApplicationIdsForCollaboratorFetcherModule extends PlaySpec with MockitoSugar with ArgumentMatchersSugar {

  object ApplicationIdsForCollaboratorFetcherMock {
    val aMock = mock[ApplicationIdsForCollaboratorFetcher]

    object FetchAllApplicationIds {

      def willReturnApplicationIds(appIds: String*) = {
        when(aMock.fetch(*)(*)).thenReturn(Future.successful(appIds.toSeq.map(ApplicationId(_))))
      }

      def willThrowException(e: Exception) = {
        when(aMock.fetch(*)(*)).thenReturn(Future.failed(e))
      }
    }
  }

}
