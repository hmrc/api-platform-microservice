/*
 * Copyright 2021 HM Revenue & Customs
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

import org.mockito.ArgumentMatchersSugar
import uk.gov.hmrc.apiplatformmicroservice.util.AsyncHmrcSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RecordMetricsSpec extends AsyncHmrcSpec with ArgumentMatchersSugar {

  trait Setup {
    val testApi = API("test-metrics")
    val apiMetricsMock = mock[ApiMetrics]

    val recordMetrics: RecordMetrics = new RecordMetrics {
      override val apiMetrics: ApiMetrics = apiMetricsMock
      override val api: API = testApi
    }
  }

  "Record" should {
    "record success when future succeeds" in new Setup {
      val result = await(recordMetrics.record(Future.successful("Success")))

      result shouldBe "Success"
      verify(apiMetricsMock).recordSuccess(testApi)
    }

    "record failure when future fails" in new Setup {
      val result = intercept[RuntimeException] {
        await(recordMetrics.record(Future.failed(new RuntimeException("Failed"))))
      }

      result.getMessage shouldBe "Failed"
      verify(apiMetricsMock).recordFailure(testApi)
    }

    "record failure when future throws exception" in new Setup {
      lazy val willThrowException = throw new RuntimeException("Failed")

      val result = intercept[RuntimeException] {
        await(recordMetrics.record(willThrowException))
      }

      result.getMessage shouldBe "Failed"
      verify(apiMetricsMock).recordFailure(testApi)
    }
  }

}
