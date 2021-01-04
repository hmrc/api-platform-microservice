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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.config

import com.google.inject.{Inject, Provider, Singleton}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.AbstractThirdPartyApplicationConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class PrincipalThirdPartyApplicationConnectorConfigProvider @Inject() (override val sc: ServicesConfig)
    extends Provider[AbstractThirdPartyApplicationConnector.Config]
    with ConfigProviderHelper {

  override def get(): AbstractThirdPartyApplicationConnector.Config = {
    val serviceName = "third-party-application-principal"
    AbstractThirdPartyApplicationConnector.Config(
      serviceUrl("third-party-application")(serviceName),
      useProxy(serviceName),
      bearerToken(serviceName),
      apiKey(serviceName)
    )
  }
}

@Singleton
class SubordinateThirdPartyApplicationConnectorConfigProvider @Inject() (override val sc: ServicesConfig)
    extends Provider[AbstractThirdPartyApplicationConnector.Config]
    with ConfigProviderHelper {

  override def get(): AbstractThirdPartyApplicationConnector.Config = {
    val serviceName = "third-party-application-subordinate"
    AbstractThirdPartyApplicationConnector.Config(
      serviceUrl("third-party-application")(serviceName),
      useProxy(serviceName),
      bearerToken(serviceName),
      apiKey(serviceName)
    )
  }
}
