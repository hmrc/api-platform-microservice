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

package uk.gov.hmrc.apiplatform.modules.apis.domain.models

import uk.gov.hmrc.apiplatform.modules.common.utils.SealedTraitJsonFormatting

sealed trait SubscriptionThrottlingTier {
  lazy val description = this.toString().split("_").head
}

object SubscriptionThrottlingTier {
  case object BRONZE_SUBSCRIPTION   extends SubscriptionThrottlingTier
  case object SILVER_SUBSCRIPTION   extends SubscriptionThrottlingTier
  case object GOLD_SUBSCRIPTION     extends SubscriptionThrottlingTier
  case object PLATINUM_SUBSCRIPTION extends SubscriptionThrottlingTier
  case object RHODIUM_SUBSCRIPTION  extends SubscriptionThrottlingTier

  val values = Set[SubscriptionThrottlingTier](BRONZE_SUBSCRIPTION, SILVER_SUBSCRIPTION, GOLD_SUBSCRIPTION, PLATINUM_SUBSCRIPTION, RHODIUM_SUBSCRIPTION)

  def apply(text: String): Option[SubscriptionThrottlingTier] = {
    SubscriptionThrottlingTier.values.find(_.toString == text.toUpperCase() + "_SUBSCRIPTION")
  }

  def unsafeApply(text: String): SubscriptionThrottlingTier =
    apply(text).getOrElse(throw new RuntimeException(s"$text is not a valid Subscription Throttling Tier"))

  def description(tier: SubscriptionThrottlingTier): String = tier.description

  implicit val formatSubscriptionThrottlingTier = SealedTraitJsonFormatting.createFormatFor[SubscriptionThrottlingTier]("Subscription Throttling Tier", apply, description)
}
