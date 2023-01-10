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

package uk.gov.hmrc.apiplatformmicroservice.xmlapis.config

import com.google.inject.{AbstractModule, Provider}
import uk.gov.hmrc.apiplatformmicroservice.xmlapis.connectors.XmlApisConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}

class ConfigurationModule extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[XmlApisConnector.Config]).toProvider(classOf[XmlApisConnectorConfigProvider])
  }

}

@Singleton
class XmlApisConnectorConfigProvider @Inject() (sc: ServicesConfig) extends Provider[XmlApisConnector.Config] {

  override def get(): XmlApisConnector.Config = {
    lazy val baseUrl = sc.baseUrl("api-platform-xml-services")
    XmlApisConnector.Config(baseUrl)
  }
}
