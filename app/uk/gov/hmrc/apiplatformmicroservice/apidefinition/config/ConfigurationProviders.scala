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

package uk.gov.hmrc.apiplatformmicroservice.apidefinition.config

import akka.pattern.FutureTimeoutSupport
import com.google.inject.{AbstractModule, Provider}
import com.google.inject.name.Names.named
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.connectors.{FutureTimeoutSupportImpl, PrincipalApiDefinitionConnector, SubordinateApiDefinitionConnector}
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.services.{ApiDefinitionService, PrincipalApiDefinitionService, SubordinateApiDefinitionService}
import uk.gov.hmrc.apiplatformmicroservice.common.ServicesConfigBridgeExtension
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.apiplatformmicroservice.common.connectors.AuthConnector
import uk.gov.hmrc.play.bootstrap.config.RunMode

class ConfigurationModule extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[FutureTimeoutSupport]).to(classOf[FutureTimeoutSupportImpl])
    bind(classOf[PrincipalApiDefinitionConnector.Config]).toProvider(classOf[PrincipalApiDefinitionConnectorConfigProvider])
    bind(classOf[SubordinateApiDefinitionConnector.Config]).toProvider(classOf[SubordinateApiDefinitionConnectorConfigProvider])
    bind(classOf[SubordinateApiDefinitionService.Config]).toProvider(classOf[SubordinateApiDefinitionServiceConfigProvider])

    bind(classOf[ApiDefinitionService]).annotatedWith(named("subordinate")).to(classOf[SubordinateApiDefinitionService])
    bind(classOf[ApiDefinitionService]).annotatedWith(named("principal")).to(classOf[PrincipalApiDefinitionService])

    bind(classOf[AuthConnector.Config]).toProvider(classOf[AuthConfigProvider])
  }
}

@Singleton
class PrincipalApiDefinitionConnectorConfigProvider @Inject() (sc: ServicesConfig) extends Provider[PrincipalApiDefinitionConnector.Config] {
  override def get(): PrincipalApiDefinitionConnector.Config = {
    lazy val principalBaseUrl = sc.baseUrl("api-definition-principal")
    PrincipalApiDefinitionConnector.Config(baseUrl = principalBaseUrl)
  }
}

@Singleton
class SubordinateApiDefinitionConnectorConfigProvider @Inject() (override val sc: ServicesConfig, configuration: Configuration)
    extends Provider[SubordinateApiDefinitionConnector.Config]
    with ServicesConfigBridgeExtension {

  override def get(): SubordinateApiDefinitionConnector.Config = {
    val subordinateServiceName = "api-definition-subordinate"
    val subordinateBaseUrl =
      serviceUrl("api-definition")(subordinateServiceName)
    val subordinateUseProxy = useProxy(subordinateServiceName)
    val subordinateBearerToken = bearerToken(subordinateServiceName)
    val subordiateApiKey = apiKey(subordinateServiceName)

    SubordinateApiDefinitionConnector.Config(
      serviceBaseUrl = subordinateBaseUrl,
      useProxy = subordinateUseProxy,
      bearerToken = subordinateBearerToken,
      apiKey = subordiateApiKey
    )
  }
}

@Singleton
class SubordinateApiDefinitionServiceConfigProvider @Inject() (configuration: Configuration) extends Provider[SubordinateApiDefinitionService.Config] {
  override def get(): SubordinateApiDefinitionService.Config = {
    val isSubordinateAvailable = configuration.getOptional[Boolean]("features.isSubordinateAvailable").getOrElse(false)
    SubordinateApiDefinitionService.Config(enabled = isSubordinateAvailable)
  }
}

@Singleton
class AuthConfigProvider @Inject()(val runModeConfiguration: Configuration, runMode: RunMode)
  extends ServicesConfig(runModeConfiguration, runMode)
  with Provider[AuthConnector.Config] {

  override def get() = {
    val url = baseUrl("auth")
    val userRole = getString("roles.user")
    val superUserRole = getString("roles.super-user")
    val adminRole = getString("roles.admin")
    val enabled = getConfBool("auth.enabled", true)
    val authorisationKey = getString("authorisationKey")

    AuthConnector.Config(url, userRole, superUserRole, adminRole, enabled, authorisationKey)
  }
}

object ConfigHelper {
  def getConfig[T](key: String, f: String => Option[T]): T = {
    f(key).getOrElse(throw new RuntimeException(s"[$key] is not configured!"))
  }
}

