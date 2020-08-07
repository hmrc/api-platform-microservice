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
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.{SubscriptionFieldsConnector, ThirdPartyApplicationConnector}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.Environment.PRODUCTION
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.Environment

@Singleton
class EnvironmentAwareConnectorsSupplier @Inject() (
    @Named("subordinate") val subordinateApplicationConnector: ThirdPartyApplicationConnector,
    @Named("principal") val principalApplicationConnector: ThirdPartyApplicationConnector,
    @Named("subordinate") val subordinateSubscriptionFieldsConnector: SubscriptionFieldsConnector,
    @Named("principal") val principalSubscriptionFieldsConnector: SubscriptionFieldsConnector) {

  def forEnvironment(environment: Environment): EnvironmentAwareConnectors = {
    environment match {
      case PRODUCTION => EnvironmentAwareConnectors(principalApplicationConnector, principalSubscriptionFieldsConnector)
      case _          => EnvironmentAwareConnectors(subordinateApplicationConnector, subordinateSubscriptionFieldsConnector)
    }
  }
}

case class EnvironmentAwareConnectors(thirdPartyApplicationConnector: ThirdPartyApplicationConnector, apiSubscriptionFieldsConnector: SubscriptionFieldsConnector)
