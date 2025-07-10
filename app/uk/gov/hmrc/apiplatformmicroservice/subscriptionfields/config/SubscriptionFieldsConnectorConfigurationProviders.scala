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

package uk.gov.hmrc.apiplatformmicroservice.subscriptionfields.config

import com.google.inject.{Inject, Provider, Singleton}

import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import uk.gov.hmrc.apiplatformmicroservice.common.config.ConfigProviderHelper
import uk.gov.hmrc.apiplatformmicroservice.subscriptionfields.connectors.{PrincipalSubscriptionFieldsConnector, SubordinateSubscriptionFieldsConnector}

@Singleton
class PrincipalSubscriptionFieldsConnectorConfigProvider @Inject() (override val sc: ServicesConfig)
    extends Provider[PrincipalSubscriptionFieldsConnector.Config]
    with ConfigProviderHelper {

  override def get(): PrincipalSubscriptionFieldsConnector.Config = {
    val serviceName = "subscription-fields-principal"
    PrincipalSubscriptionFieldsConnector.Config(
      serviceUrl("subscription-fields")(serviceName)
    )
  }
}

@Singleton
class SubordinateSubscriptionFieldsConnectorConfigProvider @Inject() (override val sc: ServicesConfig)
    extends Provider[SubordinateSubscriptionFieldsConnector.Config]
    with ConfigProviderHelper {

  override def get(): SubordinateSubscriptionFieldsConnector.Config = {
    val serviceName = "subscription-fields-subordinate"
    SubordinateSubscriptionFieldsConnector.Config(
      serviceUrl("subscription-fields")(serviceName),
      useProxy(serviceName),
      bearerToken(serviceName),
      apiKey(serviceName)
    )
  }
}
