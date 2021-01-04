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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.domain

import org.joda.time.DateTime
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.UserId

case class UserResponse(userId: UserId,
                         email: String,
                        firstName: String,
                        lastName: String,
                        registrationTime: DateTime,
                        lastModified: DateTime,
                        verified: Boolean)

object UserResponse {

  import play.api.libs.json.JodaReads.DefaultJodaDateTimeReads
  import play.api.libs.json.JodaWrites.JodaDateTimeWrites

  implicit val dateTimeFormat: Format[DateTime] = Format(DefaultJodaDateTimeReads, JodaDateTimeWrites)
  implicit val format = Json.format[UserResponse]
}
