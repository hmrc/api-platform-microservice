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
import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models._

@Singleton
class ApiDefinitionsForApplicationFetcher @Inject() (
    apiDefinitionService: EnvironmentAwareApiDefinitionService
  )(implicit ec: ExecutionContext
  ) extends FilterDevHubSubscriptions with FilterGateKeeperSubscriptions {

  def fetch(environment: Environment, subscriptions: Set[ApiIdentifier], restricted: Boolean)(implicit hc: HeaderCarrier): Future[List[ApiDefinition]] = {
    if (restricted) {
      fetchRestricted(environment, subscriptions)
    } else {
      fetchUnrestricted(environment)
    }
  }

  def fetchRestricted(environment: Environment, subscriptions: Set[ApiIdentifier])(implicit hc: HeaderCarrier): Future[List[ApiDefinition]] = {
    for {
      defs <- apiDefinitionService(environment).fetchAllNonOpenAccessApiDefinitions
    } yield filterApisForDevHubSubscriptions(subscriptions)(defs)
  }

  def fetchUnrestricted(environment: Environment)(implicit hc: HeaderCarrier): Future[List[ApiDefinition]] = {
    for {
      defs <- apiDefinitionService(environment).fetchAllNonOpenAccessApiDefinitions
    } yield filterApisForGateKeeperSubscriptions(defs)
  }
}
