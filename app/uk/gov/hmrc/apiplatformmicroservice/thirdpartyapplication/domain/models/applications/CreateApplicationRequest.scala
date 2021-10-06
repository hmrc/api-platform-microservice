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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications

import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.Environment
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.controllers.domain.UpliftData

case class CreateApplicationRequest(
  name: String,
  access: Access = Standard(List.empty, None, None, Set.empty),
  description: Option[String] = None,
  environment: Environment,
  collaborators: Set[Collaborator],
  upliftData: Option[UpliftData]) {

  def normaliseCollaborators: CreateApplicationRequest = {
    val normalised = collaborators.map(c => c.copy(emailAddress = c.emailAddress.toLowerCase))
    copy(collaborators = normalised)
  }

  require(name.nonEmpty, "name is required")
  require(collaborators.exists(_.role == Role.ADMINISTRATOR), "at least one ADMINISTRATOR collaborator is required")
  require(collaborators.size == collaborators.map(_.emailAddress.toLowerCase).size, "duplicate email in collaborator")
  access match {
    case a: Standard => require(a.redirectUris.size <= 5, "maximum number of redirect URIs exceeded")
    case _ =>
  }
}
