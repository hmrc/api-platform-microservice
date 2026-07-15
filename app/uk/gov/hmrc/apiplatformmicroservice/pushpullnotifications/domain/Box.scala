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

import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, ClientId, Environment}

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

enum SubscriptionType {
  case ApiPushSubscriber, ApiPullSubscriber
}

object SubscriptionType {
  def apply(text: String): Option[SubscriptionType] = SubscriptionType.values.find(_.toString.equalsIgnoreCase(text))

  import play.api.libs.json.Format
  import uk.gov.hmrc.apiplatform.modules.common.domain.services.SimpleEnumJsonFormatting
  implicit val format: Format[SubscriptionType] = SimpleEnumJsonFormatting.createEnumFormatFor[SubscriptionType]("SubscriptionType", apply)
}

sealed trait Subscriber {
  val subscribedDateTime: Instant
  val subscriptionType: SubscriptionType
}
