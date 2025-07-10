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

import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationWithCollaborators, ApplicationWithSubscriptionFields}
import uk.gov.hmrc.apiplatformmicroservice.common.Recoveries
import uk.gov.hmrc.apiplatformmicroservice.subscriptionfields.connectors.EnvironmentAwareSubscriptionFieldsConnector
import uk.gov.hmrc.apiplatformmicroservice.subscriptionfields.services.SubscriptionFieldsService
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.EnvironmentAwareThirdPartyApplicationConnector

@Singleton
class ApplicationByIdFetcher @Inject() (
    thirdPartyApplicationConnector: EnvironmentAwareThirdPartyApplicationConnector,
    subscriptionFieldsConnector: EnvironmentAwareSubscriptionFieldsConnector,
    subscriptionFieldsService: SubscriptionFieldsService
  )(implicit ec: ExecutionContext
  ) extends Recoveries {

  def fetchApplication(id: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[ApplicationWithCollaborators]] = {
    val subordinateApp: Future[Option[ApplicationWithCollaborators]] = thirdPartyApplicationConnector.subordinate.fetchApplication(id) recover recoverWithDefault(None)
    val principalApp: Future[Option[ApplicationWithCollaborators]]   = thirdPartyApplicationConnector.principal.fetchApplication(id)

    for {
      subordinate <- subordinateApp
      principal   <- principalApp
    } yield principal.orElse(subordinate)
  }

  def fetchApplicationWithSubscriptionData(id: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[ApplicationWithSubscriptionFields]] = {
    import cats.data.OptionT
    import cats.implicits._

    val foapp = fetchApplication(id)

    (
      for {
        app          <- OptionT(foapp)
        subs         <- OptionT.liftF(thirdPartyApplicationConnector(app.deployedTo).fetchSubscriptionsById(app.id))
        filledFields <- OptionT.liftF(subscriptionFieldsService.fetchFieldValuesWithDefaults(app.deployedTo, app.clientId, subs))
      } yield ApplicationWithSubscriptionFields(app.details, app.collaborators, subs, filledFields)
    ).value
  }
}
