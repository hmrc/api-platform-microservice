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

package uk.gov.hmrc.apiplatformmicroservice.common

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.Environment._
import uk.gov.hmrc.apiplatformmicroservice.common.utils.HmrcSpec

class EnvironmentAwareSpec extends HmrcSpec with MockitoSugar with ArgumentMatchersSugar {

  trait Setup {

    trait Something {}

    val subordinateSomething = mock[Something]
    val principalSomething   = mock[Something]

    val eatpac = new EnvironmentAware[Something] {
      def subordinate: Something = subordinateSomething
      def principal: Something   = principalSomething
    }
  }

  "EnvironmentAwareConnector" should {
    "return the principal when asked for production" in new Setup {
      eatpac(PRODUCTION) shouldBe principalSomething
    }
    "return the subordinate when asked for anything other than production" in new Setup {
      eatpac(SANDBOX) shouldBe subordinateSomething
    }
  }
}
