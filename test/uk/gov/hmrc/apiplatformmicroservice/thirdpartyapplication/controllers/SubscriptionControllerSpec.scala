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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.controllers

import java.time.Clock
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future.successful

import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.testkit.NoMaterializer

import play.api.libs.json.Json
import play.api.test.Helpers.{contentAsJson, status, _}
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.services.ClockNow
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationWithCollaboratorsFixtures
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiDefinitionTestDataHelper
import uk.gov.hmrc.apiplatformmicroservice.common.connectors.AuthConnector
import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks._
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.UpliftApplicationService

class SubscriptionControllerSpec extends AsyncHmrcSpec with ApiDefinitionTestDataHelper with ClockNow with ApplicationIdFixtures with ApiIdentifierFixtures {

  val clock = Clock.systemUTC()

  trait Setup extends ApplicationByIdFetcherModule with ApplicationWithCollaboratorsFixtures {
    implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
    implicit val mat: Materializer            = NoMaterializer

    val mockAuthConfig               = mock[AuthConnector.Config]
    val mockAuthConnector            = mock[AuthConnector]
    val mockUpliftApplicationService = mock[UpliftApplicationService]

    val controller = new SubscriptionController(
      ApplicationByIdFetcherMock.aMock,
      mockAuthConfig,
      mockAuthConnector,
      Helpers.stubControllerComponents(),
      mockUpliftApplicationService
    )
  }

  "fetchUpliftableSubscriptions" should {

    "return OK with a list of upliftable subscriptions" when {
      "there are upliftable apis available for the application id" in new Setup {
        val application = standardApp.inSandbox()
        ApplicationByIdFetcherMock.FetchApplicationWithSubscriptionData.willReturnApplicationWithSubscriptionData(application, Set(apiIdentifierOne, apiIdentifierTwo))

        val apiIdentifiers = Set(apiIdentifierOne)
        when(mockUpliftApplicationService.fetchUpliftableApisForApplication(*)(*)).thenReturn(successful(apiIdentifiers))

        val result = controller.fetchUpliftableSubscriptions(ApplicationId.random)(FakeRequest())

        status(result) shouldBe OK
        contentAsJson(result) shouldBe Json.toJson(apiIdentifiers)
      }
    }

    "return NotFound" when {
      "there are no upliftable apis available for the application id" in new Setup {
        val application = standardApp.inSandbox()
        ApplicationByIdFetcherMock.FetchApplicationWithSubscriptionData.willReturnApplicationWithSubscriptionData(application, Set(apiIdentifierOne, apiIdentifierTwo))

        when(mockUpliftApplicationService.fetchUpliftableApisForApplication(*)(*)).thenReturn(successful(Set.empty[ApiIdentifier]))

        val result = controller.fetchUpliftableSubscriptions(ApplicationId.random)(FakeRequest())

        status(result) shouldBe NOT_FOUND
      }
    }
  }
}
