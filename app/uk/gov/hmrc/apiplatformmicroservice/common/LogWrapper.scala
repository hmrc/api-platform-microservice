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

package uk.gov.hmrc.apiplatformmicroservice.common

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

trait AbstractLogWrapper {

  def logFn(message: => String, error: => Throwable): Unit

  def log[A](failMessage: Throwable => String)(f: => Future[A])(implicit ec: ExecutionContext): Future[A] = {
    f.onComplete {
      case Failure(throwable) if NonFatal(throwable) => logFn(failMessage(throwable), throwable)
      case Success(_)                                => ()
    }
    f
  }
}

trait LogWrapper extends AbstractLogWrapper {

  def logFn(message: => String, error: => Throwable): Unit =
    play.api.Logger.warn(message, error)
}

object LogWrapper extends LogWrapper
