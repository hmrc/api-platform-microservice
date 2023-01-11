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

package uk.gov.hmrc.apiplatformmicroservice.metrics

import javax.inject.{Inject, Provider, Singleton}

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.{DisabledMetrics, Metrics, MetricsImpl}

trait ApiMetrics {
  def recordFailure(api: API): Unit
  def recordSuccess(api: API): Unit
  def startTimer(api: API): Timer
}

trait Timer {
  def stop(): Unit
}

trait BaseApiMetrics extends ApiMetrics {
  val metrics: Metrics

  val metricsRegistry: MetricRegistry = metrics.defaultRegistry

  def recordFailure(api: API): Unit =
    metricsRegistry.counter(api.name ++ "-failed-counter").inc()

  def recordSuccess(api: API): Unit =
    metricsRegistry.counter(api.name ++ "-success-counter").inc()

  def startTimer(api: API): Timer = {
    val context = metricsRegistry.timer(api.name ++ "-timer").time()

    new Timer {
      def stop(): Unit = context.stop
    }
  }
}

@Singleton
class ApiMetricsProvider @Inject() (inboundMetrics: Metrics) extends Provider[ApiMetrics] {

  def get(): ApiMetrics = {
    inboundMetrics match {
      case m: MetricsImpl     => new ApiMetricsImpl(m)
      case _: DisabledMetrics => new NoopApiMetrics
    }
  }
}

class ApiMetricsImpl(val metrics: Metrics) extends BaseApiMetrics

object NoopTimer extends Timer {
  def stop() = {}
}

class NoopApiMetrics extends ApiMetrics {
  override def recordFailure(api: API) = ()
  override def recordSuccess(api: API) = ()
  override def startTimer(api: API)    = NoopTimer
}
