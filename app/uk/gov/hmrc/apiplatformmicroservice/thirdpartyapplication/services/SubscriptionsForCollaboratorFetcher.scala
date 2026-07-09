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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpReads.Implicits.*

import uk.gov.hmrc.apiplatform.modules.common.domain.models.*
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationWithSubscriptions
import uk.gov.hmrc.apiplatform.modules.applications.query.domain.models.ApplicationQueries
import uk.gov.hmrc.apiplatformmicroservice.common.Recoveries
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.QueryConnector

@Singleton
class SubscriptionsForCollaboratorFetcher @Inject() (
    queryConnector: QueryConnector
  )(implicit ec: ExecutionContext
  ) extends Recoveries {

  def fetch(userId: UserId)(implicit hc: HeaderCarrier): Future[Set[ApiIdentifier]] = {
    val qry = ApplicationQueries.applicationsByUserId(userId, wantSubscriptions = true)

    val subordinateSubscriptions =
      queryConnector.query[List[ApplicationWithSubscriptions]](Environment.Sandbox, qry).map(_.map(_.subscriptions).flatten.toSet) recover recoverWithDefault(
        Set.empty[ApiIdentifier]
      )
    val principalSubscriptions   = queryConnector.query[List[ApplicationWithSubscriptions]](Environment.Production, qry).map(_.map(_.subscriptions).flatten.toSet)

    for {
      subordinate <- subordinateSubscriptions
      principal   <- principalSubscriptions
    } yield subordinate ++ principal
  }
}
