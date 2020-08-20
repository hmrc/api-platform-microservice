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

package uk.gov.hmrc.apiplatformmicroservice.apidefinition.services

import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.APIDefinition
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiVersionDefinition
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.APIStatus
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.PrivateApiAccess

trait FilterApis {

  def filterApis(applicationIds: Seq[ApplicationId])(apis: Seq[APIDefinition]): Seq[APIDefinition] = {
    apis.filterNot(_.requiresTrust).flatMap(filterVersions(applicationIds))
  }

  private def filterVersions(applicationIds: Seq[ApplicationId])(api: APIDefinition): Option[APIDefinition] = {
    def activeVersions(version: ApiVersionDefinition): Boolean = version.status != APIStatus.RETIRED

    def visiblePrivateVersions(version: ApiVersionDefinition): Boolean = version.access match {
      case PrivateApiAccess(_, true)                      => true
      case PrivateApiAccess(whitelistedApplicationIds, _) => whitelistedApplicationIds.exists(applicationIds.contains(_))
      case _                                              => true
    }

    val filteredVersions = api.versions.filter(v => activeVersions(v) && visiblePrivateVersions(v))

    if (filteredVersions.isEmpty) None
    else Some(api.copy(versions = filteredVersions))
  }
}
