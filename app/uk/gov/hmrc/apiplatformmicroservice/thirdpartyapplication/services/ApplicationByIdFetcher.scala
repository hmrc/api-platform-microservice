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

import uk.gov.hmrc.apiplatformmicroservice.common.Recoveries
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.ApplicationId
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.{EnvironmentAwareSubscriptionFieldsConnector, EnvironmentAwareThirdPartyApplicationConnector}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications._

@Singleton
class ApplicationByIdFetcher @Inject() (
    thirdPartyApplicationConnector: EnvironmentAwareThirdPartyApplicationConnector,
    subscriptionFieldsConnector: EnvironmentAwareSubscriptionFieldsConnector,
    subscriptionFieldsService: SubscriptionFieldsService
  )(implicit ec: ExecutionContext
  ) extends Recoveries {

  def fetchApplication(id: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[Application]] = {
    val subordinateApp: Future[Option[Application]] = thirdPartyApplicationConnector.subordinate.fetchApplication(id) recover recoverWithDefault(None)
    val principalApp: Future[Option[Application]]   = thirdPartyApplicationConnector.principal.fetchApplication(id)

    for {
      subordinate <- subordinateApp
      principal   <- principalApp
    } yield principal.orElse(subordinate)
  }

  def fetchApplicationWithSubscriptionData(id: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[ApplicationWithSubscriptionData]] = {
    import cats.data.OptionT
    import cats.implicits._

    val foapp = fetchApplication(id)

    (
      for {
        app          <- OptionT(foapp)
        subs         <- OptionT.liftF(thirdPartyApplicationConnector(app.deployedTo).fetchSubscriptionsById(app.id))
        filledFields <- OptionT.liftF(subscriptionFieldsService.fetchFieldValuesWithDefaults(app.deployedTo, app.clientId, subs))
      } yield ApplicationWithSubscriptionData(app, subs, filledFields)
    ).value
  }
}
