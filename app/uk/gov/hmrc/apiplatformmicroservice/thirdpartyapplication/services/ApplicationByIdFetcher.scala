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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.apiplatformmicroservice.common.Recoveries
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications._
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.{EnvironmentAwareSubscriptionFieldsConnector, EnvironmentAwareThirdPartyApplicationConnector}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.services.EnvironmentAwareApiDefinitionService
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiStatus
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiIdentifier

@Singleton
class ApplicationByIdFetcher @Inject() (
    thirdPartyApplicationConnector: EnvironmentAwareThirdPartyApplicationConnector,
    subscriptionFieldsConnector: EnvironmentAwareSubscriptionFieldsConnector,
    subscriptionFieldsFetcher: SubscriptionFieldsFetcher,
    apiDefinitionService: EnvironmentAwareApiDefinitionService
  )(implicit ec: ExecutionContext)
    extends Recoveries {

  def fetchApplication(id: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[Application]] = {
    val subordinateApp: Future[Option[Application]] = thirdPartyApplicationConnector.subordinate.fetchApplication(id) recover recoverWithDefault(None)
    val principalApp: Future[Option[Application]] = thirdPartyApplicationConnector.principal.fetchApplication(id)

    for {
      subordinate <- subordinateApp
      principal <- principalApp
    } yield principal.orElse(subordinate)
  }

  def fetchApplicationWithSubscriptionData(id: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[ApplicationWithSubscriptionData]] = {
    import cats.data.OptionT
    import cats.implicits._

    def findRetiredApis(): Future[List[ApiIdentifier]] = {
      for {
        fdefs <- apiDefinitionService.subordinate.fetchAllApiDefinitions
        flattened = fdefs.flatMap(d => {
          d.versions.flatMap(v => {
            List( (d.context, v.version, v.status) )
          })
        })
      }
      yield flattened
      .filter(_._3 == ApiStatus.RETIRED)
      .map(t => ApiIdentifier(t._1, t._2))
    }

    val foapp = fetchApplication(id)

    (
      for {
        app <- OptionT(foapp)
        subs <- OptionT.liftF(thirdPartyApplicationConnector(app.deployedTo).fetchSubscriptionsById(app.id))
        retired <- OptionT.liftF(findRetiredApis)
        goodSubs = subs.filterNot(retired.contains)
        filledFields <- OptionT.liftF(subscriptionFieldsFetcher.fetchFieldValuesWithDefaults(app.deployedTo, app.clientId, goodSubs))
      } yield ApplicationWithSubscriptionData(app, goodSubs, filledFields)
    ).value
  }
}