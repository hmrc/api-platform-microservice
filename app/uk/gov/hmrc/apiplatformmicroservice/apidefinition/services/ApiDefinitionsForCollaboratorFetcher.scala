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

package uk.gov.hmrc.apiplatformmicroservice.apidefinition.services

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models._
import uk.gov.hmrc.apiplatformmicroservice.common.Recoveries
import uk.gov.hmrc.apiplatform.modules.developers.domain.models.UserId
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.ApplicationId
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.{ApplicationIdsForCollaboratorFetcher, SubscriptionsForCollaboratorFetcher}
import uk.gov.hmrc.apiplatform.modules.apis.domain.models._

@Singleton
class ApiDefinitionsForCollaboratorFetcher @Inject() (
    principalDefinitionService: PrincipalApiDefinitionService,
    subordinateDefinitionService: SubordinateApiDefinitionService,
    appIdsFetcher: ApplicationIdsForCollaboratorFetcher,
    subscriptionsForCollaborator: SubscriptionsForCollaboratorFetcher
  )(implicit ec: ExecutionContext
  ) extends Recoveries with FilterApiDocumentation {

  def fetch(developerId: Option[UserId])(implicit hc: HeaderCarrier): Future[List[ApiDefinition]] = {
    val principalDefinitionsFuture   = principalDefinitionService.fetchAllApiDefinitions
    val subordinateDefinitionsFuture = subordinateDefinitionService.fetchAllApiDefinitions recover recoverWithDefault(List.empty[ApiDefinition])

    for {
      principalDefinitions       <- principalDefinitionsFuture
      subordinateDefinitions     <- subordinateDefinitionsFuture
      combinedDefinitions         = combineDefinitions(principalDefinitions, subordinateDefinitions)
      collaboratorApplicationIds <- developerId.fold(successful(Set.empty[ApplicationId]))(appIdsFetcher.fetch)
      collaboratorSubscriptions  <- developerId.fold(successful(Set.empty[ApiIdentifier]))(subscriptionsForCollaborator.fetch)
    } yield filterApisForDocumentation(collaboratorApplicationIds, collaboratorSubscriptions)(combinedDefinitions)
  }

  private def combineDefinitions(principalDefinitions: List[ApiDefinition], subordinateDefinitions: List[ApiDefinition]): List[ApiDefinition] = {
    subordinateDefinitions ++ principalDefinitions.filterNot(pd => subordinateDefinitions.exists(sd => sd.serviceName == pd.serviceName))
  }
}
