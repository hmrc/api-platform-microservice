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

package uk.gov.hmrc.apiplatformmicroservice.common.builder

import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.Collaborator
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.Role

trait CollaboratorsBuilder {
  def buildCollaborators(collaborators: Seq[(String, Role)]): Set[Collaborator] = {
    collaborators.map(
      n => Collaborator(n._1, n._2)
    ).toSet
  }
  def buildCollaborator(email: String, role: Role): Collaborator = {
    Collaborator(email, role)
  }
}