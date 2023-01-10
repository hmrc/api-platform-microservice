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

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.AddCollaboratorResult
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.{AddCollaborator, AddCollaboratorRequest, RemoveCollaborator, RemoveCollaboratorRequest}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.ApplicationCollaboratorService
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.Future

trait ApplicationCollaboratorServiceModule extends MockitoSugar with ArgumentMatchersSugar {

  object ApplicationCollaboratorServiceMock {
    val aMock = mock[ApplicationCollaboratorService]

    object handleRequestCommand {

      def willReturnAddCollaborator(addCollaborator: AddCollaborator) = {
        when(aMock.handleRequestCommand(*, any[AddCollaboratorRequest])(*)).thenReturn(Future.successful(addCollaborator))
      }

      def willReturnRemoveCollaborator(removeCollaborator: RemoveCollaborator) = {
        when(aMock.handleRequestCommand(*, any[RemoveCollaboratorRequest])(*)).thenReturn(Future.successful(removeCollaborator))
      }

      def willReturnErrorsAddCollaborator(): Unit = {
        when(aMock.handleRequestCommand(*, any[AddCollaboratorRequest])(*)).thenReturn(Future.failed(UpstreamErrorResponse("some error", 404)))
      }

      def willReturnErrorsRemoveCollaborator(): Unit = {
        when(aMock.handleRequestCommand(*, any[RemoveCollaboratorRequest])(*)).thenReturn(Future.failed(UpstreamErrorResponse("some error", 404)))
      }

    }

    object AddCollaborator {

      def willReturnAddCollaboratorResponse(addCollaboratorResponse: AddCollaboratorResult) = {
        when(aMock.addCollaborator(*, *, *, *)(*)).thenReturn(Future.successful(addCollaboratorResponse))
      }
    }
  }
}
