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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications

import org.joda.time.DateTime
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.{ApiContext, ApiIdentifier, ApiVersion}
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models._
import java.util.UUID

case class ClientId(value: String) extends AnyVal
object ClientId {
  def empty: ClientId = ClientId("")
  def random: ClientId = ClientId(UUID.randomUUID().toString)
}

case class Collaborator(emailAddress: String, role: Role, userId: Option[UserId])

case class FieldValue(value: String) extends AnyVal


case class Application(
    id: ApplicationId,
    clientId: ClientId,
    gatewayId: String,
    name: String,
    createdOn: DateTime,
    lastAccess: DateTime,
    lastAccessTokenUsage: Option[DateTime] = None, // API-4376: Temporary inclusion whilst Server Token functionality is retired
    deployedTo: Environment,
    description: Option[String] = None,
    collaborators: Set[Collaborator] = Set.empty,
    access: Access = Standard(),
    state: ApplicationState = ApplicationState.testing,
    rateLimitTier: String = "BRONZE",
    blocked: Boolean = false,
    checkInformation: Option[CheckInformation] = None,
    ipAllowlist: IpAllowlist = IpAllowlist())

case class ApplicationWithSubscriptionData(
    application: Application,
    subscriptions: Set[ApiIdentifier] = Set.empty,
    subscriptionFieldValues: Map[ApiContext, Map[ApiVersion, Map[FieldName, FieldValue]]] = Map.empty)

object Application {
  implicit val ordering: Ordering[Application] = Ordering.by(_.name)
}
