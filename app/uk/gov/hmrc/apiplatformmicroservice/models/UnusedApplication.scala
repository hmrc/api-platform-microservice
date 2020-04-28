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
import play.api.libs.json.{Format, JsError, JsPath, JsResult, JsString, JsSuccess, JsValue, Json, Reads, Writes}
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import play.api.libs.functional.syntax._
import uk.gov.hmrc.apiplatformmicroservice.models.Environment.Environment

case class ApplicationUsageDetails(applicationId: UUID, creationDate: DateTime, lastAccessDate: Option[DateTime])

case class UnusedApplication(applicationId: UUID, environment: Environment, lastInteractionDate: DateTime)

object Environment extends Enumeration {
  type Environment = Value
  val SANDBOX, PRODUCTION = Value
}

object MongoFormat {
  implicit val dateFormat = ReactiveMongoFormats.dateTimeFormats

  implicit def environmentWrites: Writes[Environment.Value] = (v: Environment.Value) => JsString(v.toString)

  val unusedApplicationReads: Reads[UnusedApplication] = (
    (JsPath \ "applicationId").read[UUID] and
      (JsPath \ "environment").read[Environment] and
      (JsPath \ "lastInteractionDate").read[DateTime]
    )(UnusedApplication.apply _)

  def environmentReads[Environment](): Reads[Environment.Value] = {
    case JsString("SANDBOX") => JsSuccess(Environment.SANDBOX)
    case JsString("PRODUCTION") => JsSuccess(Environment.PRODUCTION)
    case JsString(s) =>
      try {
        JsSuccess(Environment.withName(s))
      } catch {
        case _: NoSuchElementException =>
          JsError(s"Enumeration expected of type: Environment, but it does not contain '$s'")
      }
    case _ => JsError("String value expected")
  }

  implicit val unusedApplicationFormat = Format(unusedApplicationReads, Json.writes[UnusedApplication])
  implicit val environmentFormat: Format[Environment.Value] = Format(environmentReads(), environmentWrites)

}