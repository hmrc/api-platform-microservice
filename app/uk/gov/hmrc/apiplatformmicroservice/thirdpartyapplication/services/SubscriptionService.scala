/*
 * Copyright 2022 HM Revenue & Customs
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
import uk.gov.hmrc.http.HeaderCarrier
import scala.concurrent.Future
import scala.concurrent.Future.successful
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.services.FilterGateKeeperSubscriptions
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.Application
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.services.ApiDefinitionsForApplicationFetcher
import scala.concurrent.ExecutionContext
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.SubscriptionService.CreateSubscriptionResult
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.SubscriptionService.CreateSubscriptionSuccess
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.SubscriptionService.CreateSubscriptionDenied
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.SubscriptionService.CreateSubscriptionDuplicate
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.EnvironmentAwareThirdPartyApplicationConnector
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models._
import uk.gov.hmrc.apiplatform.modules.subscriptions.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.EnvironmentAwareSubscriptionFieldsConnector

@Singleton
class SubscriptionService @Inject()(
  apiDefinitionsForApplicationFetcher: ApiDefinitionsForApplicationFetcher,
  thirdPartyApplicationConnector: EnvironmentAwareThirdPartyApplicationConnector,
  subscriptionFieldsConnector: EnvironmentAwareSubscriptionFieldsConnector,
  subscriptionFieldsFetcher: SubscriptionFieldsFetcher
)(implicit ec: ExecutionContext) extends FilterGateKeeperSubscriptions {

  @deprecated("remove after clients are no longer using the old endpoint")
  def createSubscriptionForApplication(application: Application, existingSubscriptions: Set[ApiIdentifier], newSubscriptionApiIdentifier: ApiIdentifier, restricted: Boolean)(implicit hc: HeaderCarrier): Future[CreateSubscriptionResult] = {
    def isPublic(in: ApiVersionDefinition) = in.access match {
      case PublicApiAccess() => true
      case _ => false      
    }
    
    def removePrivateVersions(in: Seq[ApiDefinition]): Seq[ApiDefinition] = 
      in.map(d => d.copy(versions = d.versions.filter(isPublic))).filterNot(_.versions.isEmpty)

    def canSubscribe(allowedSubscriptions : Seq[ApiDefinition], newSubscriptionApiIdentifier: ApiIdentifier) : Boolean = {
      val allVersions : Seq[ApiIdentifier] = allowedSubscriptions.flatMap(api => api.versions.map(version => ApiIdentifier(api.context, version.version)))

      allVersions.contains(newSubscriptionApiIdentifier)
    }

    def isSubscribed(existingSubscriptions: Set[ApiIdentifier], newSubscriptionApiIdentifier: ApiIdentifier) : Boolean = {
      existingSubscriptions.contains(newSubscriptionApiIdentifier)
    }
    
    apiDefinitionsForApplicationFetcher.fetch(application, existingSubscriptions, restricted)
      .flatMap(possibleSubscriptions => {
        
        val allowedSubscriptions = if(restricted) removePrivateVersions(possibleSubscriptions) else possibleSubscriptions

        (canSubscribe(allowedSubscriptions, newSubscriptionApiIdentifier), isSubscribed(existingSubscriptions, newSubscriptionApiIdentifier)) match {
          case (_, true) => successful(CreateSubscriptionDuplicate)
          case (false, _) => successful(CreateSubscriptionDenied)
          case _ => subscribeToApiAndCreateFieldValues(application, newSubscriptionApiIdentifier)
        }
      }
    )
  }

  def createManySubscriptionsForApplication(application: Application, apis: Set[ApiIdentifier])(implicit hc: HeaderCarrier): Future[CreateSubscriptionResult] = {
   
    def canSubscribeToAll(allowedSubscriptions : Seq[ApiDefinition]) : Boolean = {
      val allowedApiIdentifiers : Seq[ApiIdentifier] = allowedSubscriptions.flatMap(api => api.versions.map(version => ApiIdentifier(api.context, version.version)))

      (apis -- allowedApiIdentifiers) isEmpty
    }

    apiDefinitionsForApplicationFetcher.fetch(application, Set.empty, false)
      .flatMap(possibleSubscriptions => {
        
        if(canSubscribeToAll(possibleSubscriptions)) {
          createFieldValuesForGivenApis(application, apis)
        } else {
          successful(CreateSubscriptionDenied)
        }
      }
    )
  }

  private def createFieldValuesForGivenApis(application: Application, apiIdentifiers: Set[ApiIdentifier])(implicit hc: HeaderCarrier): Future[CreateSubscriptionResult] = {
    Future.sequence(apiIdentifiers.map(api => createFieldValues(application, api)))
    .map(_ => CreateSubscriptionSuccess)
  }

  private def createFieldValues(application: Application, apiIdentifier: ApiIdentifier)(implicit hc: HeaderCarrier): Future[CreateSubscriptionResult] = {
    for {
      fieldValues         <- subscriptionFieldsFetcher.fetchFieldValuesWithDefaults(application.deployedTo, application.clientId, Set(apiIdentifier))
      fieldValuesForApi    = ApiFieldMap.extractApi(apiIdentifier)(fieldValues)
      fvResuls            <- subscriptionFieldsConnector(application.deployedTo).saveFieldValues(application.clientId, apiIdentifier, fieldValuesForApi)
    } yield CreateSubscriptionSuccess
  }

  private def subscribeToApiAndCreateFieldValues(application: Application, apiIdentifier: ApiIdentifier)(implicit hc: HeaderCarrier): Future[CreateSubscriptionResult] = {
    for {
      _ <- createFieldValues(application, apiIdentifier)
      subscribeApiResult  <- thirdPartyApplicationConnector(application.deployedTo).subscribeToApi(application.id, apiIdentifier)
    } yield CreateSubscriptionSuccess
  }
}

object SubscriptionService {
  trait CreateSubscriptionResult

  case object CreateSubscriptionSuccess extends CreateSubscriptionResult
  case object CreateSubscriptionDenied extends CreateSubscriptionResult
  case object CreateSubscriptionDuplicate extends CreateSubscriptionResult
}
