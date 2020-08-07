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

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.{ApiContext, ApiIdentifier, ApiVersion}
import uk.gov.hmrc.apiplatformmicroservice.common.Recoveries
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApplicationByIdFetcher @Inject() (
    thirdPartyApplicationConnector: EnvironmentAwareThirdPartyApplicationConnector,
    subscriptionFieldsConnector: EnvironmentAwareSubscriptionFieldsConnector
  )(implicit ec: ExecutionContext)
    extends Recoveries {

  def fetch(id: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[ApplicationWithSubscriptionData]] = {
    import cats.data.OptionT
    import cats.implicits._

    val subordinateApp: Future[Option[Application]] = thirdPartyApplicationConnector.subordinate.fetchApplication(id) recover recoverWithDefault(None)
    val principalApp: Future[Option[Application]] = thirdPartyApplicationConnector.principal.fetchApplication(id)

    val foapp = for {
      subordinate <- subordinateApp
      principal <- principalApp
    } yield principal.orElse(subordinate)

    def fetchFieldValuesForAllSubscribedApis(app: Application, ids: Set[ApiIdentifier]): Future[Map[ApiContext, Map[ApiVersion, Map[FieldName, FieldValue]]]] = {
      def byApiIdentifier(id: ApiIdentifier)(fields: Map[FieldName, FieldValue]): Map[ApiContext, Map[ApiVersion, Map[FieldName, FieldValue]]] =
        if (fields.isEmpty)
          Map.empty
        else
          Map(id.context -> Map(id.version -> fields))

      Future.sequence(
        ids
          .toList
          .map(id =>
            subscriptionFieldsConnector(app.deployedTo).fetchFieldValues(app.clientId, id)
              .map(fs => byApiIdentifier(id)(fs))
          )
      )
      // roll up all the maps into one big map
        .map(ms => ms.foldLeft(Map.empty[ApiContext, Map[ApiVersion, Map[FieldName, FieldValue]]])((acc, m) => acc ++ m))
    }

    (
      for {
        app <- OptionT(foapp)
        subs <- OptionT.liftF(thirdPartyApplicationConnector(app.deployedTo).fetchSubscriptions(app.id))
        fields <- OptionT.liftF(fetchFieldValuesForAllSubscribedApis(app, subs)) // TODO - should we return all field values even for those not subscribed to
      } yield ApplicationWithSubscriptionData.fromApplication(app, subs, fields)
    )
      .value
  }
}
