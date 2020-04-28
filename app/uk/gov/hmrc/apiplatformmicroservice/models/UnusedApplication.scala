/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.apiplatformmicroservice.models

import java.util.UUID

import org.joda.time.DateTime
import play.api.libs.json.{Format, JsPath, Json, Reads}
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import play.api.libs.functional.syntax._

case class ApplicationUsageDetails(applicationId: UUID, creationDate: DateTime, lastAccessDate: Option[DateTime])

case class UnusedApplication(applicationId: UUID, lastInteractionDate: DateTime)

object MongoFormat {
  implicit val dateFormat = ReactiveMongoFormats.dateTimeFormats

  val unusedApplicationReads: Reads[UnusedApplication] = (
    (JsPath \ "applicationId").read[UUID] and
      (JsPath \ "lastInteractionDate").read[DateTime]
    )(UnusedApplication.apply _)

  implicit val unusedApplicationFormat = {
    Format(unusedApplicationReads, Json.writes[UnusedApplication])
  }
}