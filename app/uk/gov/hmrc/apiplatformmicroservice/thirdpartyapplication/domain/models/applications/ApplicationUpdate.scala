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

import play.api.libs.json.Json
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.UserId
import uk.gov.hmrc.play.json.Union

import java.time.LocalDateTime

sealed trait ApplicationUpdate {
  def timestamp: LocalDateTime
}

trait GatekeeperApplicationUpdate extends ApplicationUpdate {
   def gatekeeperUser: String
}


case class AddCollaboratorRequest(email: String, version: String, timestamp: LocalDateTime) extends ApplicationUpdate
case class AddCollaboratorGatekeeperRequest(gatekeeperUser: String, email: String, version: String, timestamp: LocalDateTime) extends GatekeeperApplicationUpdate
case class AddCollaborator(instigator: UserId, email: String,  collaborator: Collaborator, adminsToEmail:Set[String], timestamp: LocalDateTime) extends ApplicationUpdate
case class AddCollaboratorGatekeeper(gatekeeperUser: String, collaborator: Collaborator, adminsToEmail:Set[String], timestamp: LocalDateTime) extends GatekeeperApplicationUpdate

trait ApplicationUpdateRequestFormatters {
  implicit val addCollaboratorRequestFormatter = Json.format[AddCollaboratorRequest]
  implicit val addCollaboratorGatekeeperRequestFormatter = Json.format[AddCollaboratorGatekeeperRequest]
  implicit val addCollaboratorFormatter = Json.format[AddCollaborator]
  implicit val addCollaboratorGatekeeperFormatter = Json.format[AddCollaboratorGatekeeper]

  implicit val applicationUpdateRequestFormatter = Union.from[ApplicationUpdate]("updateType")
    .and[AddCollaboratorRequest]("addCollaboratorRequest")
    .and[AddCollaboratorGatekeeperRequest]("addCollaboratorGatekeeperRequest")
    .and[AddCollaborator]("addCollaborator")
    .and[AddCollaboratorGatekeeper]("addCollaboratorGatekeeper")
    .format

}
