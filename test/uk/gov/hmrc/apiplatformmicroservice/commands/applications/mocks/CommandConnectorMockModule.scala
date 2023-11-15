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

import scala.concurrent.Future.successful

import cats.data.NonEmptyList
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, LaxEmailAddress}
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.commands.applications.connectors._
import uk.gov.hmrc.apiplatformmicroservice.commands.applications.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.Application

trait CommandConnectorMockModule {
  self: MockitoSugar with ArgumentMatchersSugar =>

  trait CommandConnectorMock[T <: AppCmdConnector] {
    def aMock: T
    val Types = AppCmdHandlerTypes

    object IssueCommand {
      import cats.syntax.either._

      def verifyNoCommandsIssued() = {
        verify(aMock, never).dispatch(*[ApplicationId], *)(*)
      }

      def verifyCalledWith(cmd: ApplicationCommand, emails: Set[LaxEmailAddress]) = {
        verify(aMock, atLeastOnce).dispatch(*[ApplicationId], eqTo(DispatchRequest(cmd, emails)))(*)
      }

      object Dispatch {

        val mockResult = mock[DispatchSuccessResult]

        def succeeds() = {
          when(aMock.dispatch(*[ApplicationId], *)(*)).thenReturn(successful(mockResult.asRight[Types.Failures]))
        }

        def succeedsWith(application: Application) = {
          when(aMock.dispatch(*[ApplicationId], *)(*)).thenReturn(successful(DispatchSuccessResult(application).asRight[Types.Failures]))
        }

        def failsWith(failure: CommandFailure, failures: CommandFailure*) = {
          when(aMock.dispatch(*[ApplicationId], *)(*)).thenReturn(successful(NonEmptyList.of(failure, failures: _*).asLeft[DispatchSuccessResult]))
        }
      }
    }
  }

  object CommandConnectorMocks {

    object Prod extends CommandConnectorMock[PrincipalAppCmdConnector] {
      val aMock = mock[PrincipalAppCmdConnector]
    }

    object Sandbox extends CommandConnectorMock[SubordinateAppCmdConnector] {
      val aMock = mock[SubordinateAppCmdConnector]
    }
  }
}
