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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.config

import com.google.inject.{AbstractModule, Provider}
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.ThirdPartyApplicationConnector.ThirdPartyApplicationConnectorConfig
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class ConfigurationModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[ThirdPartyApplicationConnectorConfig]).toProvider(classOf[ThirdPartyApplicationConnectorConfigProvider])
  }
}

@Singleton
class ThirdPartyApplicationConnectorConfigProvider @Inject()(sc: ServicesConfig)
  extends Provider[ThirdPartyApplicationConnectorConfig] {

  private def serviceUrl(key: String)(serviceName: String): String = {
    if (useProxy(serviceName)) s"${sc.baseUrl(serviceName)}/${sc.getConfString(s"$serviceName.context", key)}"
    else sc.baseUrl(serviceName)
  }

  private def useProxy(serviceName: String) = sc.getConfBool(s"$serviceName.use-proxy", false)

  private def bearerToken(serviceName: String) = sc.getConfString(s"$serviceName.bearer-token", "")

  private def apiKey(serviceName: String) = sc.getConfString(s"$serviceName.api-key", "")

  override def get(): ThirdPartyApplicationConnectorConfig = {
    ThirdPartyApplicationConnectorConfig(
      serviceUrl("third-party-application")("third-party-application-sandbox"),
      useProxy("third-party-application-sandbox"),
      bearerToken("third-party-application-sandbox"),
      apiKey("third-party-application-sandbox"),
      serviceUrl("third-party-application")("third-party-application-production"),
      useProxy("third-party-application-production"),
      bearerToken("third-party-application-production"),
      apiKey("third-party-application-production")
    )
  }
}
