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
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApplicationByIdFetcher @Inject() (
    @Named("subordinate") subordinateTpaConnector: ThirdPartyApplicationConnector,
    @Named("principal") principalTpaConnector: ThirdPartyApplicationConnector,
    environmentAwareConnectorsSupplier: EnvironmentAwareConnectorsSupplier,
    subscriptionFieldsService: SubscriptionFieldsService
  )(implicit ec: ExecutionContext)
    extends Recoveries {

  def fetch(id: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[ApplicationWithSubscriptionData]] = {
    import cats.data.OptionT
    import cats.implicits._

    val subordinateApp: Future[Option[Application]] = subordinateTpaConnector.fetchApplication(id) recover recoverWithDefault(None)
    val principalApp: Future[Option[Application]] = principalTpaConnector.fetchApplication(id)

    val foapp = for {
      subordinate <- subordinateApp
      principal <- principalApp
    } yield principal.orElse(subordinate)

    (
      for {
        app <- OptionT(foapp)
        connector = environmentAwareConnectorsSupplier.forEnvironment(app.deployedTo).thirdPartyApplicationConnector
        subs <- OptionT.liftF(connector.fetchSubscriptions(app.id))
        // fields <- Future.sequence(subs.map(_ => subscriptionFieldsService.fetchFieldsValues(app, _.apiIdentifier)))
      } yield ApplicationWithSubscriptionData.fromApplication(app, subs, Map.empty)
    )
      .value
  }
}
