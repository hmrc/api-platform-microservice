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

import akka.actor.{ActorSystem, Scheduler}
import akka.pattern.FutureTimeoutSupport
import uk.gov.hmrc.apiplatformmicroservice.util.AsyncHmrcSpec
import uk.gov.hmrc.http.BadRequestException

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

class RetriesSpec extends AsyncHmrcSpec {

  class Setup(retryCount: Int, retryDelayMilliseconds: Int) {
    var actualDelay: Option[FiniteDuration] = None
    val mockFutureTimeoutSupport: FutureTimeoutSupport =
      new FutureTimeoutSupport {
        override def after[T](duration: FiniteDuration, using: Scheduler)(
            value: => Future[T])(implicit ec: ExecutionContext): Future[T] = {
          actualDelay = Some(duration)
          value
        }
      }

    def underTest = new RetryTestConnector(
      mockFutureTimeoutSupport,
      retryCount,
      retryDelayMilliseconds
    )
  }

  class RetryTestConnector(val futureTimeout: FutureTimeoutSupport,
                           val retryCount: Int,
                           val retryDelayMilliseconds: Int)
      extends Retries {
    implicit val ec: ExecutionContext = global

    override protected def actorSystem: ActorSystem =
      ActorSystem("test-actor-system")
  }

  "Retries" should {

    "wait for the configured delay before retrying" in new Setup(1, 250) {
      private val expectedDelayMilliseconds = 250
      private val expectedDelay =
        FiniteDuration(expectedDelayMilliseconds, TimeUnit.MILLISECONDS)

      intercept[BadRequestException] {
        await(underTest.retry {
          Future.failed(new BadRequestException(""))
        })
      }
      actualDelay shouldBe Some(expectedDelay)
    }

    "Retry the configured number of times on Bad Request" in new Setup(3, 10) {
      var actualRetries = 0

      private val response: Unit = await(underTest.retry {
        if (actualRetries < 3) {
          actualRetries += 1
          Future.failed(new BadRequestException(""))
        } else Future.successful(())
      })

      response shouldBe ((): Unit)
      actualRetries shouldBe 3
    }

    "Not retry when retryCount is configured to zero" in new Setup(0, 10) {
      var actualCalls = 0

      intercept[BadRequestException](await(underTest.retry {
        actualCalls += 1
        Future.failed(new BadRequestException(""))
      }))

      actualCalls shouldBe 1
    }

    "Not retry on exceptions other than BadRequestException" in new Setup(3, 10) {
      var actualCalls = 0

      intercept[RuntimeException](await(underTest.retry {
        actualCalls += 1
        Future.failed(new RuntimeException(""))
      }))

      actualCalls shouldBe 1
    }
  }
}
