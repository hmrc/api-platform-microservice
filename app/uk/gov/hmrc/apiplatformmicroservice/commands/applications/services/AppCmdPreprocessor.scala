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

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

import cats.data.NonEmptyList

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress
import uk.gov.hmrc.apiplatform.modules.common.services.EitherTHelper
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.common.ApplicationLogger
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.Application

/*
 * Do any preprocessing and/or validating of the request
 */

trait AbstractAppCmdPreprocessor[C] {
  implicit def ec: ExecutionContext

  def process(app: Application, cmd: C, data: Set[LaxEmailAddress])(implicit hc: HeaderCarrier): AppCmdPreprocessorTypes.AppCmdResultT

  val E = EitherTHelper.make[NonEmptyList[CommandFailure]]
}

@Singleton
class AppCmdPreprocessor @Inject() (
    subscribeToApiPreprocessor: SubscribeToApiPreprocessor
  )(implicit val ec: ExecutionContext
  ) extends ApplicationLogger {

  val E = EitherTHelper.make[NonEmptyList[CommandFailure]]

  def process(app: Application, dispatchRequest: DispatchRequest)(implicit hc: HeaderCarrier): AppCmdPreprocessorTypes.AppCmdResultT = {
    import ApplicationCommands._

    dispatchRequest.command match {
      case cmd: SubscribeToApi => subscribeToApiPreprocessor.process(app, cmd, dispatchRequest.verifiedCollaboratorsToNotify)

      case _ => E.pure(dispatchRequest)
    }
  }
}
