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

import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.common.Recoveries
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.ThirdPartyApplicationConnector

@Singleton
class SubscriptionsForCollaboratorFetcher @Inject() (
    @Named("subordinate") subordinateTpaConnector: ThirdPartyApplicationConnector,
    @Named("principal") principalTpaConnector: ThirdPartyApplicationConnector
  )(implicit ec: ExecutionContext
  ) extends Recoveries {

  def fetch(userId: UserId)(implicit hc: HeaderCarrier): Future[Set[ApiIdentifier]] = {
    val subordinateSubscriptions = subordinateTpaConnector.fetchSubscriptions(userId).map(_.toSet) recover recoverWithDefault(Set.empty[ApiIdentifier])
    val principalSubscriptions   = principalTpaConnector.fetchSubscriptions(userId).map(_.toSet)

    for {
      subordinate <- subordinateSubscriptions
      principal   <- principalSubscriptions
    } yield subordinate ++ principal
  }
}
