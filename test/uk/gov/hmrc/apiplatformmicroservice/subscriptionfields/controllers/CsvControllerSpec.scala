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

package uk.gov.hmrc.apiplatformmicroservice.subscriptionfields.controllers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future.successful

import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.common.domain.models.Environment
import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.apiplatformmicroservice.subscriptionfields.connectors.{EnvironmentAwareSubscriptionFieldsConnector, SubscriptionFieldsConnector}

class CsvControllerSpec extends AsyncHmrcSpec {

  trait Setup {
    implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

    val envAwareConnector  = mock[EnvironmentAwareSubscriptionFieldsConnector]
    val subsFieldConnector = mock[SubscriptionFieldsConnector]

    when(envAwareConnector.apply(*)).thenReturn(subsFieldConnector)

    val controller = new CsvController(
      Helpers.stubControllerComponents(),
      envAwareConnector
    )
  }

  "CsvController" should {
    "return CSV" in new Setup {
      val expectedCsv = "A,B\n1,2\n3,4"
      when(subsFieldConnector.csv()(*)).thenReturn(successful(expectedCsv))

      val result = controller.csv(Environment.PRODUCTION)(FakeRequest())

      contentAsString(result) shouldBe expectedCsv
      contentType(result).value shouldBe "text/csv"

    }
  }
}
