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

import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationWithCollaborators
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.SubordinateApplicationFetcher

trait SubordinateApplicationFetcherModule extends MockitoSugar with ArgumentMatchersSugar {

  object SubordinateApplicationFetcherMock {
    val aMock = mock[SubordinateApplicationFetcher]

    object FetchSubordinateApplication {

      def willReturnApplication(application: ApplicationWithCollaborators) =
        when(aMock.fetchSubordinateApplication(*[ApplicationId])(*)).thenReturn(Future.successful(Some(application)))

      def willReturnNothing = when(aMock.fetchSubordinateApplication(*[ApplicationId])(*)).thenReturn(Future.successful(None))
    }
  }
}
