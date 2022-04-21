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

package uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.domain.services

import uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.domain._
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.services.ApplicationJsonFormatters


trait PushPullNotificationJsonFormatters extends ApplicationJsonFormatters {
  import play.api.libs.json._  
  
  val dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
  implicit val JodaDateReads: Reads[org.joda.time.DateTime] = JodaReads.jodaDateReads(dateFormat)
  implicit val JodaDateWrites: Writes[org.joda.time.DateTime] = JodaWrites.jodaDateWrites(dateFormat)
  implicit val JodaDateTimeFormat: Format[org.joda.time.DateTime] = Format(JodaDateReads, JodaDateWrites)

  implicit val formatBoxCreator: Format[BoxCreator] = Json.format[BoxCreator]
  implicit val formatBoxSubscriber: Format[BoxSubscriber]  = Json.format[BoxSubscriber]
  implicit val formatBox: Format[Box] = Json.format[Box] // TODO: Can we delete this?
  implicit val formatBox2: Format[Box2] = Json.format[Box2]
}

object PushPullNotificationJsonFormatters extends PushPullNotificationJsonFormatters

