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

package uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.domain

import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.ClientId
import org.joda.time.DateTime
import enumeratum.{Enum, EnumEntry, PlayJsonEnum}
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.Environment

case class Box(
  boxId: String, // TODO: Create BoxId class
  boxCreator : BoxCreator,
  applicationId : ApplicationId,
  subscriber: Option[BoxSubscriber],
  environment: Environment
)

case class BoxCreator(clientId: ClientId)
case class BoxSubscriber(
  callBackUrl: String,
  subscribedDateTime: DateTime,
  subscriptionType: SubscriptionType
)


sealed trait SubscriptionType extends EnumEntry
object SubscriptionType extends Enum[SubscriptionType] with PlayJsonEnum[SubscriptionType]  {
   val values: scala.collection.immutable.IndexedSeq[SubscriptionType] = findValues

  case object API_PUSH_SUBSCRIBER extends SubscriptionType
  case object API_PULL_SUBSCRIBER extends SubscriptionType // Does this need to exist?
}

sealed trait Subscriber {
  val subscribedDateTime: DateTime
  val subscriptionType: SubscriptionType
}