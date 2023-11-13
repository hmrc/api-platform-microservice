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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, LocalDateTime, Period}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApiIdentifier, ApplicationId, ClientId, Environment}
import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models._
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models._
import uk.gov.hmrc.apiplatform.modules.subscriptions.domain.models._

case class Application(
    id: ApplicationId,
    clientId: ClientId,
    gatewayId: String,
    name: String,
    createdOn: Instant,
    lastAccess: Option[Instant],
    grantLength: Period,
    lastAccessTokenUsage: Option[Instant] = None, // API-4376: Temporary inclusion whilst Server Token functionality is retired
    deployedTo: Environment,
    description: Option[String] = None,
    collaborators: Set[Collaborator] = Set.empty,
    access: Access = Access.Standard(),
    state: ApplicationState = ApplicationState(State.TESTING, None, None, None, updatedOn = LocalDateTime.now(Clock.systemUTC()).truncatedTo(ChronoUnit.MILLIS)),
    rateLimitTier: String = "BRONZE",
    blocked: Boolean = false,
    checkInformation: Option[CheckInformation] = None,
    ipAllowlist: IpAllowlist = IpAllowlist(),
    moreApplication: MoreApplication = MoreApplication(true)
  )

case class ApplicationWithSubscriptionData(
    application: Application,
    subscriptions: Set[ApiIdentifier] = Set.empty,
    subscriptionFieldValues: ApiFieldMap[FieldValue] = Map.empty
  )

object Application {
  implicit val ordering: Ordering[Application] = Ordering.by(_.name)
}
