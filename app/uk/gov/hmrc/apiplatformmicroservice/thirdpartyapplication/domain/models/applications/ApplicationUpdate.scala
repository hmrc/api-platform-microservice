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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.UserId
import uk.gov.hmrc.play.json.Union

import java.time.LocalDateTime

// TODO? sealed
trait ApplicationUpdate {
// TODO? def timestamp: LocalDateTime
}

trait GatekeeperApplicationUpdate extends ApplicationUpdate {
// TODO? def gatekeeperUser: String
}

case class AddCollaborator(userId: UserId, email: String, version: String, timestamp: LocalDateTime) extends ApplicationUpdate
case class AddCollaboratorGatekeeper(gatekeeperUser: String, email: String, version: String, timestamp: LocalDateTime) extends GatekeeperApplicationUpdate

case class SubscribeToApi(userId: UserId, email: String, version: String, timestamp: LocalDateTime) extends ApplicationUpdate
case class SubscribeToApiGatekeeper(gatekeeperUser: String, email: String, version: String, timestamp: LocalDateTime) extends GatekeeperApplicationUpdate

// TODO? sealed
trait ApplicationUpdateResponse

// TODO: these may have to mirror third-party-application's ApplicationData responses (?)
case class AddCollaboratorResponse(userRegistered: Boolean) extends ApplicationUpdateResponse
case class SubscribeToApiResponse(successful: Boolean) extends ApplicationUpdateResponse


trait ApplicationUpdateFormatters {
  implicit val addCollaboratorFormatter = Json.format[AddCollaborator]
  implicit val addCollaboratorGatekeeperFormatter = Json.format[AddCollaboratorGatekeeper]
  implicit val subscribeToApiFormatter = Json.format[SubscribeToApi]
  implicit val subscribeToApiGatekeeperFormatter = Json.format[SubscribeToApiGatekeeper]

  implicit val addCollaboratorResponseFormatter = Json.format[AddCollaboratorResponse]
  implicit val subscribeToApiResponseFormatter = Json.format[SubscribeToApiResponse]

  implicit val applicationUpdateRequestFormatter = Union.from[ApplicationUpdate]("updateType")
    .and[AddCollaborator]("addCollaborator")
    .and[AddCollaboratorGatekeeper]("addCollaboratorGatekeeper")
    .and[SubscribeToApi]("subscribeToApi")
    .and[SubscribeToApiGatekeeper]("subscribeToApiGatekeeper")
    .format

  implicit val applicationUpdateResponseFormatter = Union.from[ApplicationUpdateResponse]("updateType")
    .and[AddCollaboratorResponse]("addCollaboratorResponse")
    .and[SubscribeToApiResponse]("subscribeToApiResponse")
    .format
}
