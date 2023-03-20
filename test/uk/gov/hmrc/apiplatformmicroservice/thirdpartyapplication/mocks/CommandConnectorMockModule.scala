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

import cats.data.NonEmptyList
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.apiplatform.modules.applications.domain.models.ApplicationId
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.ApplicationCommandConnector
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.CommandFailure
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.ApplicationCommand
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.DispatchRequest
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.Application
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.PrincipalApplicationCommandConnector
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.SubordinateApplicationCommandConnector

trait CommandConnectorMockModule {
  self: MockitoSugar with ArgumentMatchersSugar =>

  trait CommandConnectorMock[T <: ApplicationCommandConnector] {
    def aMock: T

    object IssueCommand {
      import cats.syntax.either._
      import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.DispatchSuccessResult
      
      def verifyNoCommandsIssued() = {
        verify(aMock, never).dispatch(*[ApplicationId], *)(*)
      }

      def verifyCalledWith(cmd: ApplicationCommand, emails: Set[LaxEmailAddress]) = {
        verify(aMock, atLeastOnce).dispatch(*[ApplicationId], eqTo(DispatchRequest(cmd, emails)))(*)
      }
        
      object Dispatch {
        
        val mockResult = mock[DispatchSuccessResult]
        
        def succeeds() = {
          when(aMock.dispatch(*[ApplicationId], *)(*)).thenReturn(successful(mockResult.asRight[NonEmptyList[CommandFailure]]))
        }
        
        def succeedsWith(application: Application) = {
          when(aMock.dispatch(*[ApplicationId], *)(*)).thenReturn(successful(DispatchSuccessResult(application).asRight[NonEmptyList[CommandFailure]]))
        }

        def failsWith(failure: CommandFailure, failures: CommandFailure*) = {
          when(aMock.dispatch(*[ApplicationId], *)(*)).thenReturn(successful(NonEmptyList(failure, failures.toList).asLeft[DispatchSuccessResult]))
        }
      }
    }
  }

  object CommandConnectorMocks {

    object Prod extends CommandConnectorMock[PrincipalApplicationCommandConnector] {
      val aMock = mock[PrincipalApplicationCommandConnector]
    }

    object Sandbox extends CommandConnectorMock[SubordinateApplicationCommandConnector] {
      val aMock = mock[SubordinateApplicationCommandConnector]
    }
  }
}
