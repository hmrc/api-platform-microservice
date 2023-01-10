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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors

import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.{ApiContext, ApiIdentifier, ApiVersion}
import uk.gov.hmrc.apiplatform.modules.subscriptions.domain.models._

object SubscriptionsHelper {

  import AbstractThirdPartyApplicationConnector._

  val ContextA   = ApiContext("A")
  val ContextB   = ApiContext("B")
  val VersionOne = ApiVersion("1.0")
  val VersionTwo = ApiVersion("2.0")

  val ApiIdentifierAOne = ApiIdentifier(ContextA, VersionOne)
  val ApiIdentifierATwo = ApiIdentifier(ContextA, VersionTwo)
  val ApiIdentifierBOne = ApiIdentifier(ContextB, VersionOne)
  val ApiIdentifierBTwo = ApiIdentifier(ContextB, VersionTwo)

  val FieldNameOne = FieldName("one")
  val FieldNameTwo = FieldName("two")

  implicit class VersionWrapper(v: ApiVersion) {
    def asInner: InnerVersion = InnerVersion(v)
  }

  implicit class StringToFieldWrapper(v: String) {
    def asFieldValue: FieldValue = FieldValue(v)
  }

  val SubsVersionsForA = Seq(SubscriptionVersion(VersionOne.asInner, true), SubscriptionVersion(VersionTwo.asInner, false))
  val SubsVersionsForB = Seq(SubscriptionVersion(VersionTwo.asInner, true))

  val MixedSubscriptions: Set[ApiIdentifier] =
    Set(
      ApiIdentifierAOne,
      ApiIdentifierBTwo
    )
}
