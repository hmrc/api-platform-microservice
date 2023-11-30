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

package uk.gov.hmrc.apiplatformmicroservice.commands.applications.services

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import cats.data.EitherT

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.common.domain.models.Actors.AppCollaborator
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApiIdentifier, ApplicationId, LaxEmailAddress, UserId}
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.ApplicationCommands.{DeleteApplicationByCollaborator, SubscribeToApi}
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.CommandFailures.GenericFailure
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.DispatchRequest
import uk.gov.hmrc.apiplatformmicroservice.common.builder.ApplicationBuilder
import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec

class AppCmdPreprocessorSpec extends AsyncHmrcSpec {

  trait SetUp extends ApplicationBuilder {
    val applicationId = ApplicationId.random
    val application   = buildApplication(appId = applicationId)

    implicit val headerCarrier: HeaderCarrier         = HeaderCarrier()
    val mockSubscribeToApiPreprocessor = mock[SubscribeToApiPreprocessor]
    val appCmdPreprocessor             = new AppCmdPreprocessor(mockSubscribeToApiPreprocessor)
  }

  "AppCmdPreprocessor" should {

    "route SubscribeToApi to SubscribeToApiPreprocessor" in new SetUp {
      import cats.syntax.either._

      val subscribeToAPICommand                                 = SubscribeToApi(AppCollaborator(LaxEmailAddress("test@test.com")), ApiIdentifier.random, LocalDateTime.now())
      val dispatchRequest                                       = DispatchRequest(subscribeToAPICommand, Set.empty)
      val dispatchResult: AppCmdPreprocessorTypes.AppCmdResultT = EitherT(Future.successful(GenericFailure("Creation of field values failed").leftNel[DispatchRequest]))

      when(mockSubscribeToApiPreprocessor.process(eqTo(application), eqTo(subscribeToAPICommand), eqTo(Set.empty))(*[HeaderCarrier]))
        .thenReturn(dispatchResult)

      appCmdPreprocessor.process(application, dispatchRequest) shouldBe dispatchResult

      verify(mockSubscribeToApiPreprocessor).process(eqTo(application), eqTo(subscribeToAPICommand), eqTo(Set.empty))(*[HeaderCarrier])
    }

    "not route other commands to SubscribeToApiPreprocessor" in new SetUp {
      val command         = DeleteApplicationByCollaborator(UserId.random, "someReason", LocalDateTime.now())
      val dispatchRequest = DispatchRequest(command, Set.empty)

      appCmdPreprocessor.process(application, dispatchRequest)

      verifyZeroInteractions(mockSubscribeToApiPreprocessor)
    }
  }
}
