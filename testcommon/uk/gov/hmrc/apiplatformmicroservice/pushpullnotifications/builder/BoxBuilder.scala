/*
 * Copyright 2022 HM Revenue & Customs
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

import uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.domain.Box
import uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.connectors.domain.BoxResponse
import uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.domain.BoxCreator
import uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.domain.SubscriptionType
import uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.domain.BoxSubscriber
import uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.domain.BoxId
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.ClientId
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.Environment

import org.joda.time.DateTime

trait BoxBuilder {
  
  def buildBox(boxId: String): Box = {
    Box(BoxId(boxId),
        s"boxName-$boxId",
        buildBoxCreator(),
        Some(ApplicationId(java.util.UUID.randomUUID())),
        Some(buildSubscriber()),
        Environment.PRODUCTION)
  }

  def buildBoxResponse(boxId: String, applicationId : Option[ApplicationId] = Some(ApplicationId(java.util.UUID.randomUUID()))) : BoxResponse = {
    BoxResponse(BoxId(boxId),
                s"boxName-$boxId",
                buildBoxCreator(),
                applicationId,
                Some(buildSubscriber()))
  }

  def buildSubscriber(): BoxSubscriber = {
    BoxSubscriber("callbackUrl", new DateTime(), SubscriptionType.API_PUSH_SUBSCRIBER)
  }

  def buildBoxCreator() : BoxCreator = {
    BoxCreator(ClientId(java.util.UUID.randomUUID().toString())),
  }

  def buildBoxFromBoxResponse(boxResponse: BoxResponse, environment: Environment) : Box = {
    Box(boxResponse.boxId,
        boxResponse.boxName,
        boxResponse.boxCreator,
        boxResponse.applicationId,
        boxResponse.subscriber,
        environment)
  }
}
