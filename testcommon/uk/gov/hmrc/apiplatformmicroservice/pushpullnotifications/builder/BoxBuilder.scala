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

package uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.builder

import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, ClientId, Environment}
import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock
import uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.connectors.domain.BoxResponse
import uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.domain.{Box, BoxCreator, BoxId, BoxSubscriber, SubscriptionType}

trait BoxBuilder extends FixedClock {

  def buildBox(boxId: String): Box = {
    Box(BoxId(boxId), s"boxName-$boxId", buildBoxCreator(), Some(ApplicationId(java.util.UUID.randomUUID())), Some(buildSubscriber()), Environment.PRODUCTION)
  }

  def buildBoxResponse(boxId: String, applicationId: Option[ApplicationId] = Some(ApplicationId.random)): BoxResponse = {
    BoxResponse(BoxId(boxId), s"boxName-$boxId", buildBoxCreator(), applicationId, Some(buildSubscriber()))
  }

  def buildSubscriber(): BoxSubscriber = {
    BoxSubscriber("callbackUrl", instant, SubscriptionType.API_PUSH_SUBSCRIBER)
  }

  def buildBoxCreator(): BoxCreator = {
    BoxCreator(ClientId(java.util.UUID.randomUUID().toString()))
  }

  def buildBoxFromBoxResponse(boxResponse: BoxResponse, environment: Environment): Box = {
    Box(boxResponse.boxId, boxResponse.boxName, boxResponse.boxCreator, boxResponse.applicationId, boxResponse.subscriber, environment)
  }
}
