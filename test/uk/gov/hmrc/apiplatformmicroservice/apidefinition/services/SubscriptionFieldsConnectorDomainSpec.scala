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

package uk.gov.hmrc.apiplatformmicroservice.apidefinition.services

import org.scalatest.WordSpec

class SubscriptionFieldsConnectorDomainSpec extends WordSpec {
  
    "merging" should {
        "work" in {
            val mm1 = Map("a" -> "A", "b" -> "B")
            val mm2 = Map("a" -> "A", "c" -> "C")
            val mm3 = Map("z" -> "Z", "y" -> "Y")

            val m1 = Map(1 -> mm1)
            val m2 = Map(1 -> mm3, 2 -> mm2)
        }
    }
}
