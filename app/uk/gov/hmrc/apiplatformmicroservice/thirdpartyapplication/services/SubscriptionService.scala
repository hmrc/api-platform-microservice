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
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiIdentifier
import uk.gov.hmrc.http.HeaderCarrier
import scala.concurrent.Future
import scala.concurrent.Future.successful
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.services.FilterGateKeeperSubscriptions
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.Application
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.services.ApiDefinitionsForApplicationFetcher
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiDefinition
import scala.concurrent.ExecutionContext
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.SubscriptionService.CreateSubscriptionResult
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.SubscriptionService.CreateSubscriptionSuccess
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.SubscriptionService.CreateSubscriptionDenied
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.SubscriptionService.CreateSubscriptionDuplicate
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.EnvironmentAwareThirdPartyApplicationConnector
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.PublicApiAccess
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiVersionDefinition
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiContext
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiVersion
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.subscriptions.SubscriptionFieldsDomain
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.subscriptions.SubscriptionFieldsDomain._


@Singleton
class SubscriptionService @Inject()(
  val apiDefinitionsForApplicationFetcher: ApiDefinitionsForApplicationFetcher,
  val thirdPartyApplicationConnector: EnvironmentAwareThirdPartyApplicationConnector
)(implicit ec: ExecutionContext) extends FilterGateKeeperSubscriptions {

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
          case _ => subscribeToApi(application, newSubscriptionApiIdentifier)
        }
      }
    )
  }

  type ApiMap[V] = Map[ApiContext, Map[ApiVersion, V]]
  type FieldMap[V] = ApiMap[Map[FieldName,V]]

  private def subscribeToApi(application: Application, apiIdentifier: ApiIdentifier)(implicit hc: HeaderCarrier): Future[ApplicationUpdateSuccessful] = {

    def ensureEmptyValuesWhenNoneExists(fieldDefinitions: Seq[SubscriptionFieldsDomain.SubscriptionFieldDefinition]): Future[Unit] = {
      for {
        oldValues <- subscriptionFieldsService.fetchFieldsValues(application, fieldDefinitions, apiIdentifier)
        saveResponse <- subscriptionFieldsService.saveBlankFieldValues(application, apiIdentifier.context, apiIdentifier.version, oldValues)
      } yield saveResponse match {
        case SaveSubscriptionFieldsSuccessResponse => ()
        case error =>
          val errorMessage = s"Failed to save blank subscription field values: $error"
          throw new RuntimeException(errorMessage)
      }
    }

    def ensureSavedValuesForAnyDefinitions(defns: Seq[SubscriptionFieldsDomain.SubscriptionFieldDefinition]): Future[Unit] = {
      if (defns.nonEmpty) {
        ensureEmptyValuesWhenNoneExists(defns)
      } else {
        Future.successful(())
      }
    }

    val fieldDefinitions: Future[Seq[SubscriptionFieldsDomain.SubscriptionFieldDefinition]] = subscriptionFieldsService.getFieldDefinitions(application, apiIdentifier)

    fieldDefinitions
      .flatMap(ensureSavedValuesForAnyDefinitions)
      .flatMap(_ => thirdPartyApplicationConnector(application.deployedTo).subscribeToApi(application.id, newSubscriptionApiIdentifier).map(_ => CreateSubscriptionSuccess))
  }

}

object SubscriptionService {
  trait CreateSubscriptionResult

  case object CreateSubscriptionSuccess extends CreateSubscriptionResult
  case object CreateSubscriptionDenied extends CreateSubscriptionResult
  case object CreateSubscriptionDuplicate extends CreateSubscriptionResult
}
