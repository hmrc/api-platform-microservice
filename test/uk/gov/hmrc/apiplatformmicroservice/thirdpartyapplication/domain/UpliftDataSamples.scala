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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain

import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.controllers.domain._
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiIdentifier

trait UpliftDataSamples {
  val aResponsibleIndividual = ResponsibleIndividual("test full name", "test email address")
  val sellResellOrDistribute = SellResellOrDistribute("Yes")
  val doNotSellResellOrDistribute = SellResellOrDistribute("No")

  def makeUpliftData(subscriptions: Set[ApiIdentifier]): UpliftData = UpliftData(aResponsibleIndividual, sellResellOrDistribute, subscriptions)
  def makeUpliftData(subscriptions: ApiIdentifier*): UpliftData = makeUpliftData(subscriptions.toSet)
}
