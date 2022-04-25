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

package uk.gov.hmrc.apiplatformmicroservice.PushPullNotifications.services

import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

import uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.mocks._
import org.mockito.MockitoSugar
import org.mockito.ArgumentMatchersSugar
import uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.mocks.PushPullNotificationsConnectorModule
import uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.services.BoxFetcher
import uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.connectors.domain.BoxResponse
import uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.domain.BoxCreator
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.ClientId
import uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.domain.Box
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.Environment

class BoxFetcherSpec extends AsyncHmrcSpec {

  implicit val hc = HeaderCarrier()

  trait Setup extends PushPullNotificationsConnectorModule with MockitoSugar with ArgumentMatchersSugar {
    val fetcher = new BoxFetcher(EnvironmentAwarePushPullNotificationsConnectorMock.instance)
  }

  "BoxFetcher" when {
    "fetchAllBoxes" should {
      "return None if none from principal and subordinate" in new Setup {
          EnvironmentAwarePushPullNotificationsConnectorMock.Principal.FetchBoxes.willReturnAllBoxes(List.empty)
          EnvironmentAwarePushPullNotificationsConnectorMock.Subordinate.FetchBoxes.willReturnAllBoxes(List.empty)

          await(fetcher.fetchAllBoxes()) shouldBe List.empty
      }

      "return principal and subordinate boxes" in new Setup {
        
        val subordinateBoxResponse = BoxResponse("boxId-1", "subordinateboxName", BoxCreator(ClientId(java.util.UUID.randomUUID().toString())),ApplicationId(java.util.UUID.randomUUID()),None)
        val principalBoxResponse = BoxResponse("boxId-2", "principalBoxName", BoxCreator(ClientId(java.util.UUID.randomUUID().toString())),ApplicationId(java.util.UUID.randomUUID()),None)

      
        val subordinateBox = Box(subordinateBoxResponse.boxId,
          subordinateBoxResponse.boxName,
          subordinateBoxResponse.boxCreator,
          subordinateBoxResponse.applicationId,
          subordinateBoxResponse.subscriber,
          Environment.SANDBOX)

        val principalBox = Box(principalBoxResponse.boxId,
          principalBoxResponse.boxName,
          principalBoxResponse.boxCreator,
          principalBoxResponse.applicationId,
          principalBoxResponse.subscriber,
          Environment.PRODUCTION)

        EnvironmentAwarePushPullNotificationsConnectorMock.Subordinate.FetchBoxes.willReturnAllBoxes(List(subordinateBoxResponse))
        EnvironmentAwarePushPullNotificationsConnectorMock.Principal.FetchBoxes.willReturnAllBoxes(List(principalBoxResponse))

        await(fetcher.fetchAllBoxes()) shouldBe List(subordinateBox, principalBox)
      }
    }
  }
}
