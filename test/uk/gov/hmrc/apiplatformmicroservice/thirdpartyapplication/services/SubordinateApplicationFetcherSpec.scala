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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services

import scala.concurrent.ExecutionContext.Implicits.global

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.time.DateTimeUtils

import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.Environment
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.ApplicationId
import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.SubordinateThirdPartyApplicationConnector
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications._
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks.ThirdPartyApplicationConnectorModule
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.ClientId

class SubordinateApplicationFetcherSpec extends AsyncHmrcSpec {

  trait Setup extends ThirdPartyApplicationConnectorModule with MockitoSugar with ArgumentMatchersSugar {
    implicit val headerCarrier = HeaderCarrier()
    val subordinateAppId       = ApplicationId.random
    val principalAppId         = ApplicationId.random

    val subordinateApplication = Application(
      subordinateAppId,
      ClientId("123"),
      "gatewayId",
      "name",
      DateTimeUtils.now,
      Some(DateTimeUtils.now),
      java.time.Period.ofDays(1),
      None,
      Environment.SANDBOX,
      Some("description")
    )

    val subordinateConnector = SubordinateThirdPartyApplicationConnectorMock.aMock
    val principalConnector   = PrincipalThirdPartyApplicationConnectorMock.aMock

    val service = new SubordinateApplicationFetcher(subordinateConnector.asInstanceOf[SubordinateThirdPartyApplicationConnector], principalConnector)
  }

  "fetchSubordinateApplication" should {
    "return the subordinate application if it exists" in new Setup {
      PrincipalThirdPartyApplicationConnectorMock.GetLinkedSubordinateApplicationId.thenReturn(subordinateAppId)
      SubordinateThirdPartyApplicationConnectorMock.FetchApplicationById.willReturnApplication(subordinateApplication)

      val result = await(service.fetchSubordinateApplication(principalAppId))

      result shouldBe Some(subordinateApplication)
    }

    "return nothing if no subordinate link exists" in new Setup {
      PrincipalThirdPartyApplicationConnectorMock.GetLinkedSubordinateApplicationId.thenReturnNothing

      val result = await(service.fetchSubordinateApplication(principalAppId))

      result shouldBe None
    }

    "return nothing if subordinate app does not exist" in new Setup {
      PrincipalThirdPartyApplicationConnectorMock.GetLinkedSubordinateApplicationId.thenReturn(subordinateAppId)
      SubordinateThirdPartyApplicationConnectorMock.FetchApplicationById.willReturnNone

      val result = await(service.fetchSubordinateApplication(principalAppId))

      result shouldBe None
    }
  }
}
