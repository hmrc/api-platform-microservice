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

import uk.gov.hmrc.apiplatform.modules.developers.domain.models.UserId
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.Collaborators
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.Collaborator
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress

trait CollaboratorsBuilder {

  def buildCollaborators(collaborators: Seq[(String, Collaborators.Role)]): Set[Collaborator] = {
    collaborators.map(n => Collaborator(LaxEmailAddress(n._1), n._2, UserId.random)).toSet
  }

  def buildCollaborator(email: String, role: Collaborators.Role, userId: UserId = UserId.random): Collaborator = {
    Collaborator(LaxEmailAddress(email), role, userId)
  }
}
