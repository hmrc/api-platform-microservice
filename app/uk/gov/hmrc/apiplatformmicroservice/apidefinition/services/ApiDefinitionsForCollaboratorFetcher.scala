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

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.{APIDefinition, APIStatus, ApiVersionDefinition, PrivateApiAccess}
import uk.gov.hmrc.apiplatformmicroservice.common.Recoveries
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.ApplicationIdsForCollaboratorFetcher
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApiDefinitionsForCollaboratorFetcher @Inject() (
    principalDefinitionService: PrincipalApiDefinitionService,
    subordinateDefinitionService: SubordinateApiDefinitionService,
    appIdsFetcher: ApplicationIdsForCollaboratorFetcher
  )(implicit ec: ExecutionContext)
    extends Recoveries {

  def fetch(email: Option[String])(implicit hc: HeaderCarrier): Future[Seq[APIDefinition]] = {
    val principalDefinitionsFuture = principalDefinitionService.fetchAllDefinitions
    val subordinateDefinitionsFuture = subordinateDefinitionService.fetchAllDefinitions recover recoverWithDefault(Seq.empty[APIDefinition])

    for {
      principalDefinitions <- principalDefinitionsFuture
      subordinateDefinitions <- subordinateDefinitionsFuture
      combinedDefinitions = combineDefinitions(principalDefinitions, subordinateDefinitions)
      applicationIds <- email.fold(successful(Seq.empty[ApplicationId]))(appIdsFetcher.fetch(_))
    } yield filterApis(combinedDefinitions, applicationIds)
  }

  private def combineDefinitions(principalDefinitions: Seq[APIDefinition], subordinateDefinitions: Seq[APIDefinition]): Seq[APIDefinition] = {
    subordinateDefinitions ++ principalDefinitions.filterNot(pd => subordinateDefinitions.exists(sd => sd.serviceName == pd.serviceName))
  }

  private def filterApis(apis: Seq[APIDefinition], applicationIds: Seq[ApplicationId]): Seq[APIDefinition] = {
    apis.filterNot(_.requiresTrust).flatMap(filterVersions(_, applicationIds))
  }

  private def filterVersions(api: APIDefinition, applicationIds: Seq[ApplicationId]): Option[APIDefinition] = {
    def activeVersions(version: ApiVersionDefinition): Boolean = version.status != APIStatus.RETIRED

    def visiblePrivateVersions(version: ApiVersionDefinition): Boolean = version.access match {
      case PrivateApiAccess(_, true)                      => true
      case PrivateApiAccess(whitelistedApplicationIds, _) =>
        whitelistedApplicationIds.exists(s => applicationIds.contains(s))
      case _                                              => true
    }

    val filteredVersions = api.versions.filter(v => activeVersions(v) && visiblePrivateVersions(v))

    if (filteredVersions.isEmpty) None
    else Some(api.copy(versions = filteredVersions))
  }
}
