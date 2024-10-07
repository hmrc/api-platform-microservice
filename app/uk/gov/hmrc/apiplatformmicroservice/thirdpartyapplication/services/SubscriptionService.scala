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
import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ApiDefinition
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationWithCollaborators
import uk.gov.hmrc.apiplatform.modules.applications.subscriptions.domain.models.ApiFieldMap
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.services.{ApiDefinitionsForApplicationFetcher, FilterGateKeeperSubscriptions}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.EnvironmentAwareSubscriptionFieldsConnector
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.SubscriptionService.{CreateSubscriptionDenied, CreateSubscriptionResult, CreateSubscriptionSuccess}

@Singleton
class SubscriptionService @Inject() (
    apiDefinitionsForApplicationFetcher: ApiDefinitionsForApplicationFetcher,
    subscriptionFieldsConnector: EnvironmentAwareSubscriptionFieldsConnector,
    subscriptionFieldsService: SubscriptionFieldsService
  )(implicit ec: ExecutionContext
  ) extends FilterGateKeeperSubscriptions {

  def createManySubscriptionsForApplication(application: ApplicationWithCollaborators, apis: Set[ApiIdentifier])(implicit hc: HeaderCarrier): Future[CreateSubscriptionResult] = {

    def canSubscribeToAll(allowedSubscriptions: Seq[ApiDefinition]): Boolean = {
      val allowedApiIdentifiers: Seq[ApiIdentifier] = allowedSubscriptions.flatMap(api => api.versions.keySet.map(versionNbr => ApiIdentifier(api.context, versionNbr)))

      (apis -- allowedApiIdentifiers) isEmpty
    }

    apiDefinitionsForApplicationFetcher.fetch(application.deployedTo, Set.empty, false)
      .flatMap(possibleSubscriptions => {

        if (canSubscribeToAll(possibleSubscriptions)) {
          createFieldValuesForGivenApis(application, apis)
        } else {
          successful(CreateSubscriptionDenied)
        }
      })
  }

  private def createFieldValuesForGivenApis(
      application: ApplicationWithCollaborators,
      apiIdentifiers: Set[ApiIdentifier]
    )(implicit hc: HeaderCarrier
    ): Future[CreateSubscriptionResult] = {
    Future.sequence(apiIdentifiers.map(api => createFieldValues(application, api)))
      .map(_ => CreateSubscriptionSuccess)
  }

  private def createFieldValues(application: ApplicationWithCollaborators, apiIdentifier: ApiIdentifier)(implicit hc: HeaderCarrier): Future[CreateSubscriptionResult] = {
    for {
      fieldValues      <- subscriptionFieldsService.fetchFieldValuesWithDefaults(application.deployedTo, application.clientId, Set(apiIdentifier))
      fieldValuesForApi = ApiFieldMap.extractApi(apiIdentifier)(fieldValues)
      fvResuls         <- subscriptionFieldsConnector(application.deployedTo).saveFieldValues(application.clientId, apiIdentifier, fieldValuesForApi)
    } yield CreateSubscriptionSuccess
  }
}

object SubscriptionService {
  trait CreateSubscriptionResult

  case object CreateSubscriptionSuccess   extends CreateSubscriptionResult
  case object CreateSubscriptionDenied    extends CreateSubscriptionResult
  case object CreateSubscriptionDuplicate extends CreateSubscriptionResult
}
