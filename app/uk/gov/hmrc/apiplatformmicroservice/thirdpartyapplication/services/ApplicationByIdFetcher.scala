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
import uk.gov.hmrc.http.HttpReads.Implicits.*

import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, Environment}
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationWithCollaborators, ApplicationWithSubscriptionFields}
import uk.gov.hmrc.apiplatform.modules.applications.query.domain.models.ApplicationQuery
import uk.gov.hmrc.apiplatformmicroservice.common.Recoveries
import uk.gov.hmrc.apiplatformmicroservice.subscriptionfields.connectors.EnvironmentAwareSubscriptionFieldsConnector
import uk.gov.hmrc.apiplatformmicroservice.subscriptionfields.services.SubscriptionFieldsService
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.{EnvironmentAwareThirdPartyApplicationConnector, QueryConnector}

@Singleton
class ApplicationByIdFetcher @Inject() (
    thirdPartyApplicationConnector: EnvironmentAwareThirdPartyApplicationConnector,
    queryConnector: QueryConnector,
    subscriptionFieldsConnector: EnvironmentAwareSubscriptionFieldsConnector,
    subscriptionFieldsService: SubscriptionFieldsService
  )(implicit ec: ExecutionContext
  ) extends Recoveries {

  def fetchApplication(id: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[ApplicationWithCollaborators]] = {
    val qry                                                          = ApplicationQuery.ById(id, Nil)
    val subordinateApp: Future[Option[ApplicationWithCollaborators]] =
      queryConnector.query[Option[ApplicationWithCollaborators]](Environment.Sandbox, qry) recover recoverWithDefault(None)
    val principalApp: Future[Option[ApplicationWithCollaborators]]   = queryConnector.query[Option[ApplicationWithCollaborators]](Environment.Production, qry)

    for {
      subordinate <- subordinateApp
      principal   <- principalApp
    } yield principal.orElse(subordinate)
  }

  def fetchApplicationWithSubscriptionFields(id: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[ApplicationWithSubscriptionFields]] = {
    val qry                                                               = ApplicationQuery.ById(id, Nil, wantSubscriptions = true, wantSubscriptionFields = true)
    val subordinateApp: Future[Option[ApplicationWithSubscriptionFields]] =
      queryConnector.query[Option[ApplicationWithSubscriptionFields]](Environment.Sandbox, qry) recover recoverWithDefault(None)
    val principalApp: Future[Option[ApplicationWithSubscriptionFields]]   = queryConnector.query[Option[ApplicationWithSubscriptionFields]](Environment.Production, qry)

    for {
      subordinate <- subordinateApp
      principal   <- principalApp
    } yield principal.orElse(subordinate)
  }
}
