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

package uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.domain

import java.time.Instant

import play.api.libs.json.Format

import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, ClientId, Environment}
import uk.gov.hmrc.apiplatform.modules.common.domain.services.SealedTraitJsonFormatting

case class BoxId(value: String) extends AnyVal

case class Box(
    boxId: BoxId,
    boxName: String,
    boxCreator: BoxCreator,
    applicationId: Option[ApplicationId],
    subscriber: Option[BoxSubscriber],
    environment: Environment
  )

case class BoxCreator(clientId: ClientId)

case class BoxSubscriber(
    callBackUrl: String,
    subscribedDateTime: Instant,
    subscriptionType: SubscriptionType
  )

sealed trait SubscriptionType

object SubscriptionType {

  case object API_PUSH_SUBSCRIBER extends SubscriptionType
  case object API_PULL_SUBSCRIBER extends SubscriptionType

  val values = Set(API_PULL_SUBSCRIBER, API_PUSH_SUBSCRIBER)

  def apply(text: String): Option[SubscriptionType] = SubscriptionType.values.find(_.toString() == text.toUpperCase)

  def unsafeApply(text: String): SubscriptionType = apply(text).getOrElse(throw new RuntimeException(s"$text is not a valid Subscription Type"))

  implicit val format: Format[SubscriptionType] = SealedTraitJsonFormatting.createFormatFor[SubscriptionType]("Subscription Type", apply)
}

sealed trait Subscriber {
  val subscribedDateTime: Instant
  val subscriptionType: SubscriptionType
}
