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

package uk.gov.hmrc.apiplatformmicroservice.commands.applications.mocks

import scala.concurrent.ExecutionContext.Implicits.global

import cats.data.NonEmptyList
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress
import uk.gov.hmrc.apiplatform.modules.common.services.EitherTHelper
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationWithCollaborators
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.{DispatchRequest, _}
import uk.gov.hmrc.apiplatformmicroservice.commands.applications.services.{AppCmdPreprocessor, AppCmdPreprocessorTypes}

trait AppCmdPreprocessorMockModule {
  self: MockitoSugar with ArgumentMatchersSugar =>

  trait AbstractAppCmdPreprocessorMock {
    def aMock: AppCmdPreprocessor
    val Types = AppCmdPreprocessorTypes
    val E     = EitherTHelper.make[NonEmptyList[CommandFailure]]

    object Process {

      def verifyNotCalled() = {
        verify(aMock, never).process(*, *)(*)
      }

      def verifyCalledWith(cmd: ApplicationCommand, emails: Set[LaxEmailAddress]) = {
        verify(aMock, atLeastOnce).process(*, eqTo(DispatchRequest(cmd, emails)))(*)
      }

      def succeedsWith(request: DispatchRequest) = {
        when(aMock.process(*, *)(*)).thenReturn(E.pure(request))
      }

      def passThru() = {
        when(aMock.process(*, *)(*)).thenAnswer((_: ApplicationWithCollaborators, inbound: DispatchRequest) => E.pure(inbound))
      }

      def failsWith(failure: CommandFailure, failures: CommandFailure*) = {
        when(aMock.process(*, *)(*)).thenReturn(E.leftT(NonEmptyList.of(failure, failures: _*)))
      }
    }
  }

  object AppCmdPreprocessorMock extends AbstractAppCmdPreprocessorMock {
    val aMock = mock[AppCmdPreprocessor]
  }
}
