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

package uk.gov.hmrc.apiplatformmicroservice.apidefinition.services

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models._
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.Application
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApiDefinitionsForApplicationFetcher @Inject() (
    apiDefinitionService: EnvironmentAwareApiDefinitionService
  )(implicit ec: ExecutionContext)
    extends FilterDevHubSubscriptions with FilterGateKeeperSubscriptions {

  def fetch(application: Application, subscriptions: Set[ApiIdentifier], restricted: Boolean)(implicit hc: HeaderCarrier): Future[List[APIDefinition]] = {
    if(restricted) {
      fetchRestricted(application, subscriptions)
    }
    else {
      fetchUnrestricted(application)
    }
  }

  def fetchRestricted(application: Application, subscriptions: Set[ApiIdentifier])(implicit hc: HeaderCarrier): Future[List[APIDefinition]] = {
    val environment = application.deployedTo
    for {
      defs <- apiDefinitionService(environment).fetchAllNonOpenAccessApiDefinitions
    } yield filterApisForDevHubSubscriptions(Set(application.id), subscriptions)(defs)
  }

  def fetchUnrestricted(application: Application)(implicit hc: HeaderCarrier): Future[List[APIDefinition]] = {
    val environment = application.deployedTo
    for {
      defs <- apiDefinitionService(environment).fetchAllNonOpenAccessApiDefinitions
    } yield filterApisForGateKeeperSubscriptions(Set(application.id))(defs)
  }
}
