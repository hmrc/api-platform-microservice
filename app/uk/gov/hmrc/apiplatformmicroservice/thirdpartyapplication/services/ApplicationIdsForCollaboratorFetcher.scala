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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services

import javax.inject.{Inject, Named, Singleton}
import uk.gov.hmrc.apiplatformmicroservice.common.Recoveries
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.ThirdPartyApplicationConnector
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApplicationIdsForCollaboratorFetcher @Inject() (
    @Named("subordinate") subordinateTpaConnector: ThirdPartyApplicationConnector,
    @Named("principal") principalTpaConnector: ThirdPartyApplicationConnector
  )(implicit ec: ExecutionContext)
    extends Recoveries {

  def fetch(email: String)(implicit hc: HeaderCarrier): Future[Set[ApplicationId]] = {
    val subordinateAppIds = subordinateTpaConnector.fetchApplicationsByEmail(email) recover recoverWithDefault(Set.empty[ApplicationId])
    val principalAppIds = principalTpaConnector.fetchApplicationsByEmail(email)

    for {
      subordinate <- subordinateAppIds
      principal <- principalAppIds
    } yield (subordinate ++ principal).toSet
  }
}
