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

package uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.config

import com.google.inject.AbstractModule
import com.google.inject.name.Names.named

import uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.connectors._

class ConfigurationModule extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[PrincipalPushPullNotificationsConnector.Config]).toProvider(classOf[PrincipalPushPullNotificationsConnectorConfigProvider])
    bind(classOf[SubordinatePushPullNotificationsConnector.Config]).toProvider(classOf[SubordinatePushPullNotificationsConnectorConfigProvider])

    bind(classOf[PushPullNotificationsConnector]).annotatedWith(named("subordinate")).to(classOf[SubordinatePushPullNotificationsConnector])
    bind(classOf[PushPullNotificationsConnector]).annotatedWith(named("principal")).to(classOf[PrincipalPushPullNotificationsConnector])
  }
}
