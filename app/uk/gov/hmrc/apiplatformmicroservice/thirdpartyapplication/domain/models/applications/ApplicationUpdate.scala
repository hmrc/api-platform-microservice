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

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.UserId
import uk.gov.hmrc.play.json.Union

import java.time.LocalDateTime

sealed trait ActorType extends EnumEntry

object ActorType extends Enum[ActorType] with PlayJsonEnum[ActorType] {
  val values = findValues

  final case object COLLABORATOR extends ActorType
  final case object GATEKEEPER extends ActorType
  final case object SCHEDULED_JOB extends ActorType
}

sealed trait Actor

case class GatekeeperUserActor(user: String) extends Actor

case class CollaboratorActor(email: String) extends Actor

case class ScheduledJobActor(jobId: String) extends Actor

object Actor {
  implicit val gatekeeperUserActorFormat: OFormat[GatekeeperUserActor] = Json.format[GatekeeperUserActor]
  implicit val collaboratorActorFormat: OFormat[CollaboratorActor] = Json.format[CollaboratorActor]
  implicit val scheduledJobActorFormat: OFormat[ScheduledJobActor] = Json.format[ScheduledJobActor]
  //    implicit val unknownActorFormat: OFormat[UnknownActor] = Json.format[UnknownActor]

  implicit val formatActor: OFormat[Actor] = Union.from[Actor]("actorType")
    //      .and[UnknownActor](ActorType.UNKNOWN.toString)
    .and[ScheduledJobActor](ActorType.SCHEDULED_JOB.toString)
    .and[GatekeeperUserActor](ActorType.GATEKEEPER.toString)
    .and[CollaboratorActor](ActorType.COLLABORATOR.toString)
    .format
}
sealed trait ApplicationUpdate {
  def timestamp: LocalDateTime
}
trait UpdateRequest extends ApplicationUpdate

trait GatekeeperApplicationUpdate extends ApplicationUpdate {
   def gatekeeperUser: String
}

case class AddCollaboratorRequest(actor: Actor, collaboratorEmail: String, collaboratorRole: Role, timestamp: LocalDateTime) extends UpdateRequest
case class AddCollaborator(actor: Actor, collaborator: Collaborator, adminsToEmail:Set[String], timestamp: LocalDateTime) extends ApplicationUpdate
case class RemoveCollaboratorRequest(actor: Actor, collaboratorEmail: String, collaboratorRole: Role, timestamp: LocalDateTime) extends UpdateRequest

case class RemoveCollaborator(actor: Actor, collaborator: Collaborator, adminsToEmail:Set[String], timestamp: LocalDateTime) extends ApplicationUpdate
trait ApplicationUpdateFormatters {

  implicit val collaboratorFormat = Json.format[Collaborator]
  implicit val addCollaboratorFormatter = Json.format[AddCollaborator]
  implicit val addCollaboratorUpdateRequestFormatter = Json.format[AddCollaboratorRequest]
  implicit val removeCollaboratorFormatter = Json.format[RemoveCollaborator]
  implicit val removeCollaboratorRequestFormatter = Json.format[RemoveCollaboratorRequest]


  implicit val applicationUpdateFormatter = Union.from[ApplicationUpdate]("updateType")
    .and[AddCollaboratorRequest]("addCollaboratorRequest")
    .and[AddCollaborator]("addCollaborator")
    .and[RemoveCollaborator]("removeCollaborator")
    .and[RemoveCollaboratorRequest]("removeCollaboratorRequest")
    .format

}