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

package uk.gov.hmrc.apiplatformmicroservice.apidefinition.services

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.{ApiDefinition, ApiIdentifier}
import uk.gov.hmrc.apiplatformmicroservice.common.Recoveries
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.SubscriptionsForCollaboratorFetcher
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.DeveloperIdentifier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscribedApiDefinitionsForCollaboratorFetcher @Inject() (
    apiDefinitionsForCollaboratorFetcher: ApiDefinitionsForCollaboratorFetcher,
    subscriptionsForCollaboratorFetcher: SubscriptionsForCollaboratorFetcher
  )(implicit ec: ExecutionContext)
    extends Recoveries {

  def fetch(developerId: DeveloperIdentifier)(implicit hc: HeaderCarrier): Future[List[ApiDefinition]] = {
    for {
      apiDefinitions <- apiDefinitionsForCollaboratorFetcher.fetch(Some(developerId))
      subscriptions <- subscriptionsForCollaboratorFetcher.fetch(developerId)
    } yield filterApis(apiDefinitions, subscriptions)
  }

  private def filterApis(apis: List[ApiDefinition], subscriptions: Set[ApiIdentifier]): List[ApiDefinition] = {
    apis.flatMap(filterVersions(_, subscriptions))
  }

  private def filterVersions(api: ApiDefinition, subscriptions: Set[ApiIdentifier]): Option[ApiDefinition] = {
    val filteredVersions = api.versions.filter(v => subscriptions.contains(models.ApiIdentifier(api.context, v.version)))

    if (filteredVersions.isEmpty) None
    else Some(api.copy(versions = filteredVersions))
  }
}
