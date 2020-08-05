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

import com.google.inject.name.Names.named
import com.google.inject.{AbstractModule, Provider}
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.ThirdPartyApplicationConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors._

class ConfigurationModule extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[ThirdPartyApplicationConnector.Config]).annotatedWith(
      named("principal")
    ).toProvider(classOf[PrincipalThirdPartyApplicationConnectorConfigProvider])

    bind(classOf[ThirdPartyApplicationConnector.Config]).annotatedWith(
      named("subordinate")
    ).toProvider(classOf[SubordinateThirdPartyApplicationConnectorConfigProvider])

    bind(classOf[ThirdPartyApplicationConnector]).annotatedWith(
      named("subordinate")
    ).to(classOf[SubordinateThirdPartyApplicationConnector])

    bind(classOf[ThirdPartyApplicationConnector]).annotatedWith(
      named("principal")
    ).to(classOf[PrincipalThirdPartyApplicationConnector])
  }
}

@Singleton
class PrincipalThirdPartyApplicationConnectorConfigProvider @Inject() (override val sc: ServicesConfig)
    extends Provider[ThirdPartyApplicationConnector.Config]
    with ThirdPartyApplicationConnectorConfigProvider {

  override def get(): ThirdPartyApplicationConnector.Config = {
    val serviceName = "third-party-application-principal"
    ThirdPartyApplicationConnector.Config(
      serviceUrl("third-party-application")(serviceName),
      useProxy(serviceName),
      bearerToken(serviceName),
      apiKey(serviceName)
    )
  }
}

@Singleton
class SubordinateThirdPartyApplicationConnectorConfigProvider @Inject() (override val sc: ServicesConfig)
    extends Provider[ThirdPartyApplicationConnector.Config]
    with ThirdPartyApplicationConnectorConfigProvider {

  override def get(): ThirdPartyApplicationConnector.Config = {
    val serviceName = "third-party-application-subordinate"
    ThirdPartyApplicationConnector.Config(
      serviceUrl("third-party-application")(serviceName),
      useProxy(serviceName),
      bearerToken(serviceName),
      apiKey(serviceName)
    )
  }
}

trait ThirdPartyApplicationConnectorConfigProvider {
  protected val sc: ServicesConfig

  def serviceUrl(key: String)(serviceName: String): String = {
    if (useProxy(serviceName)) s"${sc.baseUrl(serviceName)}/${sc.getConfString(s"$serviceName.context", key)}"
    else sc.baseUrl(serviceName)
  }

  def useProxy(serviceName: String) = sc.getConfBool(s"$serviceName.use-proxy", false)

  def bearerToken(serviceName: String) = sc.getConfString(s"$serviceName.bearer-token", "")

  def apiKey(serviceName: String) = sc.getConfString(s"$serviceName.api-key", "")
}
