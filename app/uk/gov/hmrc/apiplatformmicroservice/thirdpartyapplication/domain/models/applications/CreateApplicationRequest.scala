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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications

import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.{Environment}
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.ApplicationId
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.controllers.domain._
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.services.ApplicationJsonFormatters._
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ApiIdentifier

trait CreateApplicationRequest {
  def name: String
  def description: Option[String]
  def access: Access
  def collaborators: Set[Collaborator]
  def environment: Environment
  def anySubscriptions: Set[ApiIdentifier]

  protected def lowerCaseEmails(in: Set[Collaborator]): Set[Collaborator] = {
    in.map(c => c.copy(emailAddress = c.emailAddress.toLowerCase))
  }

  def validate(in: CreateApplicationRequest): Unit = {
    require(in.name.nonEmpty, "name is required")
    require(in.collaborators.exists(_.role == Role.ADMINISTRATOR), "at least one ADMINISTRATOR collaborator is required")
    require(in.collaborators.size == collaborators.map(_.emailAddress.toLowerCase).size, "duplicate email in collaborator")
    in.access match {
      case a: Standard => require(a.redirectUris.size <= 5, "maximum number of redirect URIs exceeded")
      case _           =>
    }
  }
}

case class CreateApplicationRequestV1(
    name: String,
    access: Access = Standard(List.empty, None, None, Set.empty),
    description: Option[String] = None,
    environment: Environment,
    collaborators: Set[Collaborator],
    subscriptions: Option[Set[ApiIdentifier]]
  ) extends CreateApplicationRequest {

  validate(this)

  def normaliseCollaborators: CreateApplicationRequestV1 = copy(collaborators = lowerCaseEmails(collaborators))

  def anySubscriptions: Set[ApiIdentifier] = subscriptions.getOrElse(Set.empty)
}

object CreateApplicationRequestV1 {
  import play.api.libs.json.Json

  implicit val format = Json.format[CreateApplicationRequestV1]
}

case class CreateApplicationRequestV2(
    name: String,
    access: Access = Standard(List.empty, None, None, Set.empty),
    description: Option[String] = None,
    environment: Environment,
    collaborators: Set[Collaborator],
    upliftRequest: UpliftRequest,
    requestedBy: String,
    sandboxApplicationId: ApplicationId
  ) extends CreateApplicationRequest {

  validate(this)

  lazy val anySubscriptions: Set[ApiIdentifier] = upliftRequest.subscriptions

  def normaliseCollaborators: CreateApplicationRequestV2 = copy(collaborators = lowerCaseEmails(collaborators))
}

object CreateApplicationRequestV2 {
  import play.api.libs.json.Json

  implicit val format = Json.format[CreateApplicationRequestV2]
}
