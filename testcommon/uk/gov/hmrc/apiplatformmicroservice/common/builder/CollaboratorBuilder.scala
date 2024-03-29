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

package uk.gov.hmrc.apiplatformmicroservice.common.builder

import uk.gov.hmrc.apiplatform.modules.common.domain.models.{LaxEmailAddress, UserId}
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.Collaborator

trait CollaboratorsBuilder {

  def buildCollaborators(collaborators: Seq[(LaxEmailAddress, Collaborator.Role)]): Set[Collaborator] = {
    collaborators.map(n => Collaborator(n._1, n._2, UserId.random)).toSet
  }

  def buildCollaborator(email: LaxEmailAddress, role: Collaborator.Role, userId: UserId = UserId.random): Collaborator = {
    Collaborator(email, role, userId)
  }
}
