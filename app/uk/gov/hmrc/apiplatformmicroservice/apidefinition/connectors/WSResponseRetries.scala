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

package uk.gov.hmrc.apiplatformmicroservice.apidefinition.connectors

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.pattern.FutureTimeoutSupport
import akka.stream.ActorMaterializer
import akka.stream.javadsl.Sink
import akka.util.ByteString
import play.api.Logger
import play.api.libs.ws.WSResponse

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

private[connectors] trait WSResponseRetries {

  protected def actorSystem: ActorSystem

  protected def retryCount: Int

  protected def retryDelayMilliseconds: Int

  protected def futureTimeout: FutureTimeoutSupport

  private val delay =
    FiniteDuration(retryDelayMilliseconds, TimeUnit.MILLISECONDS)

  private implicit val mat: ActorMaterializer = ActorMaterializer()(actorSystem)

  implicit val ec: ExecutionContext

  def retryWSResponse[A](block: => Future[A]): Future[A] = {
    def loop(previousRetryAttempts: Int = 0)(block: => Future[A]): Future[A] = {
      block.flatMap {
        case Some(wsResponse: WSResponse)
            if wsResponse.status == 400 && previousRetryAttempts < retryCount =>
          val retryAttempt = previousRetryAttempts + 1

          Logger.warn(
            s"Retry attempt $retryAttempt of $retryCount in $delay due to Bad Request returned from proxy")

          // Force drain of source just to be sure
          wsResponse.bodyAsSource.runWith(Sink.ignore[ByteString])

          futureTimeout.after(delay, actorSystem.scheduler)(
            loop(retryAttempt)(block))

        case x => Future.successful(x)
      }
    }

    loop()(block)
  }
}
