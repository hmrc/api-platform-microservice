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
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.connectors.ApiDefinitionConnector
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.{APIDefinition, APIStatus, APIVersion, PrivateApiAccess}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.ApplicationIdsForCollaboratorFetcher
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApiDefinitionsForCollaboratorFetcher @Inject()(apiDefinitionConnector: ApiDefinitionConnector, appIdsFetcher: ApplicationIdsForCollaboratorFetcher)
                                                    (implicit ec: ExecutionContext) {

  def apply(email: Option[String])(implicit hc: HeaderCarrier): Future[Seq[APIDefinition]] = {
    for {
      allApiDefinitions <- apiDefinitionConnector.fetchAllApiDefinitions
      applicationIds <- email.fold(successful(Seq.empty[String]))(appIdsFetcher(_))
    } yield filterApis(allApiDefinitions, applicationIds)
  }

  private def filterApis(apis: Seq[APIDefinition], applicationIds: Seq[String]): Seq[APIDefinition] = {
    def apiRequiresTrust(api: APIDefinition): Boolean = api.requiresTrust.getOrElse(false)
    apis.filterNot(apiRequiresTrust).flatMap(filterVersions(_, applicationIds))
  }

  private def filterVersions(api: APIDefinition, applicationIds: Seq[String]): Option[APIDefinition] = {
    def activeVersions(version: APIVersion): Boolean = version.status != APIStatus.RETIRED

    def visiblePrivateVersions(version: APIVersion): Boolean = version.access match {
      case PrivateApiAccess( _, true) => true
      case PrivateApiAccess(whitelistedApplicationIds, _) =>
        whitelistedApplicationIds.exists(s => applicationIds.contains(s))
      case _ => true
    }

    val filteredVersions = api.versions.filter(v => activeVersions(v) && visiblePrivateVersions(v))

    if (filteredVersions.isEmpty) None
    else Some(api.copy(versions = filteredVersions))
  }
}
