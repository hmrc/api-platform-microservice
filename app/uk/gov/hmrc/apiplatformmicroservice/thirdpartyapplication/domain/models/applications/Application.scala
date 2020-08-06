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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications

import org.joda.time.DateTime
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.{ApiContext, ApiIdentifier, ApiVersion}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models._

case class ClientId(value: String) extends AnyVal

case class Collaborator(emailAddress: String, role: Role)

case class FieldName(value: String) extends AnyVal
case class FieldValue(value: String) extends AnyVal

case class Application(
    id: ApplicationId,
    clientId: ClientId,
    name: String,
    createdOn: DateTime,
    lastAccess: DateTime,
    lastAccessTokenUsage: Option[DateTime] = None, // API-4376: Temporary inclusion whilst Server Token functionality is retired
    deployedTo: Environment,
    description: Option[String] = None,
    collaborators: Set[Collaborator] = Set.empty,
    access: Access = Standard(),
    state: ApplicationState = ApplicationState.testing,
    checkInformation: Option[CheckInformation] = None,
    ipWhitelist: Set[String] = Set.empty)

case class ApplicationWithSubscriptionData(
    id: ApplicationId,
    clientId: ClientId,
    name: String,
    createdOn: DateTime,
    lastAccess: DateTime,
    lastAccessTokenUsage: Option[DateTime] = None, // API-4376: Temporary inclusion whilst Server Token functionality is retired
    deployedTo: Environment,
    description: Option[String] = None,
    collaborators: Set[Collaborator] = Set.empty,
    access: Access = Standard(),
    state: ApplicationState = ApplicationState.testing,
    checkInformation: Option[CheckInformation] = None,
    ipWhitelist: Set[String] = Set.empty,
    subscriptions: Set[ApiIdentifier] = Set.empty,
    subscriptionFieldValues: Map[ApiContext, Map[ApiVersion, Map[FieldName, FieldValue]]] = Map.empty)

object Application {
  implicit val ordering: Ordering[Application] = Ordering.by(_.name)
}

object ApplicationWithSubscriptionData {

  def fromApplication(
      app: Application,
      subscriptions: Set[ApiIdentifier],
      subscriptionFieldValues: Map[ApiContext, Map[ApiVersion, Map[FieldName, FieldValue]]]
    ): ApplicationWithSubscriptionData = {
    ApplicationWithSubscriptionData(
      app.id,
      app.clientId,
      app.name,
      app.createdOn,
      app.lastAccess,
      app.lastAccessTokenUsage,
      app.deployedTo,
      app.description,
      app.collaborators,
      app.access,
      app.state,
      app.checkInformation,
      app.ipWhitelist,
      subscriptions,
      subscriptionFieldValues
    )
  }

}
