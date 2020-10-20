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
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiIdentifier
import uk.gov.hmrc.http.HeaderCarrier
import scala.concurrent.Future
import scala.concurrent.Future.successful
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.services.FilterGateKeeperSubscriptions
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.Application
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.services.ApiDefinitionsForApplicationFetcher
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.APIDefinition
import scala.concurrent.ExecutionContext
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.SubscriptionService.CreateSubscriptionResult
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.SubscriptionService.CreateSubscriptionSuccess
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.SubscriptionService.CreateSubscriptionDenied
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.SubscriptionService.CreateSubscriptionDuplicate
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.EnvironmentAwareThirdPartyApplicationConnector

@Singleton
class SubscriptionService @Inject()(
  val apiDefinitionsForApplicationFetcher: ApiDefinitionsForApplicationFetcher,
  val thirdPartyApplicationConnector: EnvironmentAwareThirdPartyApplicationConnector
)(implicit ec: ExecutionContext) extends FilterGateKeeperSubscriptions {

  def createSubscriptionForApplication(application: Application, existingSubscriptions: Set[ApiIdentifier], newSubscriptionApiIdentifier: ApiIdentifier)(implicit hc: HeaderCarrier): Future[CreateSubscriptionResult] = {
    def canSubscribe(allowedSubscriptions : Seq[APIDefinition], newSubscriptionApiIdentifier: ApiIdentifier) : Boolean = {
      val allVersions : Seq[ApiIdentifier] = allowedSubscriptions.map(api => api.versions.map(version => ApiIdentifier(api.context, version.version))).flatten
      allVersions.contains(newSubscriptionApiIdentifier)
    }

    def isSubscribed(existingSubscriptions: Set[ApiIdentifier], newSubscriptionApiIdentifier: ApiIdentifier) : Boolean = {
      existingSubscriptions.contains(newSubscriptionApiIdentifier)
    }
    
    apiDefinitionsForApplicationFetcher.fetchUnrestricted(application, application.deployedTo)
      .flatMap(allowedSubscriptions =>
        (canSubscribe(allowedSubscriptions, newSubscriptionApiIdentifier), isSubscribed(existingSubscriptions, newSubscriptionApiIdentifier)) match {
          case (_, true) => successful(CreateSubscriptionDuplicate)
          case (false, _) => successful(CreateSubscriptionDenied)
          case _ => thirdPartyApplicationConnector(application.deployedTo).subscribeToApi(application.id, newSubscriptionApiIdentifier).map(_ => CreateSubscriptionSuccess)
      })
  }
}

object SubscriptionService {
  trait CreateSubscriptionResult

  case object CreateSubscriptionSuccess extends CreateSubscriptionResult
  case object CreateSubscriptionDenied extends CreateSubscriptionResult
  case object CreateSubscriptionDuplicate extends CreateSubscriptionResult
}
