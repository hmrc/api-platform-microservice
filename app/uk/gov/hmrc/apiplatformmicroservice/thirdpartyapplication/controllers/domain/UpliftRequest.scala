/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.controllers.domain

import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiIdentifier

final case class ResponsibleIndividual(fullName: String, emailAddress: String)

object ResponsibleIndividual {
  import play.api.libs.json.{Format, Json}
  implicit val format: Format[ResponsibleIndividual] = Json.format[ResponsibleIndividual]
}

final case class SellResellOrDistribute(answer: String) extends AnyVal

object SellResellOrDistribute {
  import play.api.libs.json.{Format, Json}
  implicit val format: Format[SellResellOrDistribute] = Json.valueFormat[SellResellOrDistribute]
}


case class UpliftRequest(
  responsibleIndividual: ResponsibleIndividual,
  sellResellOrDistribute: SellResellOrDistribute,
  subscriptions: Set[ApiIdentifier],
  requestedBy: String
)
  
object UpliftRequest {
  import play.api.libs.json.{Format, Json, Reads}
  implicit val reads: Reads[UpliftRequest] = Json.reads[UpliftRequest]
  implicit val format: Format[UpliftRequest] = Json.format[UpliftRequest]
}