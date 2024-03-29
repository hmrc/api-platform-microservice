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

import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiDefinitionTestDataHelper
import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks.ThirdPartyApplicationConnectorModule

class SubscriptionsForCollaboratorFetcherSpec extends AsyncHmrcSpec with ApiDefinitionTestDataHelper {

  trait Setup extends ThirdPartyApplicationConnectorModule with MockitoSugar with ArgumentMatchersSugar {
    implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
    val developer                             = UserId.random

    val apiContextHelloWorld  = ApiContext("hello-world")
    val apiContextHelloAgents = ApiContext("hello-agents")
    val apiVersionOne         = ApiVersionNbr("1.0")
    val apiVersionTwo         = ApiVersionNbr("2.0")

    val subordinateSubscriptions = Seq(ApiIdentifier(apiContextHelloWorld, apiVersionOne), ApiIdentifier(apiContextHelloWorld, apiVersionTwo))
    val principalSubscriptions   = Seq(ApiIdentifier(apiContextHelloWorld, apiVersionOne), ApiIdentifier(apiContextHelloAgents, apiVersionOne))
    val underTest                = new SubscriptionsForCollaboratorFetcher(SubordinateThirdPartyApplicationConnectorMock.aMock, PrincipalThirdPartyApplicationConnectorMock.aMock)
  }

  "SubscriptionsForCollaboratorFetcher" should {
    "concatenate both subordinate and principal subscriptions without duplicates" in new Setup {
      SubordinateThirdPartyApplicationConnectorMock.FetchSubscriptionsByUserId.willReturnSubscriptions(subordinateSubscriptions: _*)
      PrincipalThirdPartyApplicationConnectorMock.FetchSubscriptionsByUserId.willReturnSubscriptions(principalSubscriptions: _*)

      val result = await(underTest.fetch(developer))

      result shouldBe Set(
        ApiIdentifier(apiContextHelloWorld, apiVersionOne),
        ApiIdentifier(apiContextHelloWorld, apiVersionTwo),
        ApiIdentifier(apiContextHelloAgents, apiVersionOne)
      )
    }

    "return subordinate subscriptions if there are no matching principal subscriptions" in new Setup {
      SubordinateThirdPartyApplicationConnectorMock.FetchSubscriptionsByUserId.willReturnSubscriptions(subordinateSubscriptions: _*)
      PrincipalThirdPartyApplicationConnectorMock.FetchSubscriptionsByUserId.willReturnSubscriptions(Seq.empty: _*)

      val result = await(underTest.fetch(developer))

      result should contain theSameElementsAs subordinateSubscriptions
    }

    "return principal subscriptions if there are no matching subordinate subscriptions" in new Setup {
      SubordinateThirdPartyApplicationConnectorMock.FetchSubscriptionsByUserId.willReturnSubscriptions(Seq.empty: _*)
      PrincipalThirdPartyApplicationConnectorMock.FetchSubscriptionsByUserId.willReturnSubscriptions(principalSubscriptions: _*)

      val result = await(underTest.fetch(developer))

      result should contain theSameElementsAs principalSubscriptions
    }

    "return an empty set if there are no matching subscriptions in any environment" in new Setup {
      SubordinateThirdPartyApplicationConnectorMock.FetchSubscriptionsByUserId.willReturnSubscriptions(Seq.empty: _*)
      PrincipalThirdPartyApplicationConnectorMock.FetchSubscriptionsByUserId.willReturnSubscriptions(Seq.empty: _*)

      val result = await(underTest.fetch(developer))

      result shouldBe Set.empty
    }

    "return principal subscriptions if something goes wrong in subordinate" in new Setup {
      val expectedExceptionMessage = "something went wrong"
      SubordinateThirdPartyApplicationConnectorMock.FetchSubscriptionsByUserId.willThrowException(new RuntimeException(expectedExceptionMessage))
      PrincipalThirdPartyApplicationConnectorMock.FetchSubscriptionsByUserId.willReturnSubscriptions(principalSubscriptions: _*)

      val result = await(underTest.fetch(developer))

      result should contain theSameElementsAs principalSubscriptions
    }

    "throw exception if something goes wrong in principal" in new Setup {
      val expectedExceptionMessage = "something went wrong"
      SubordinateThirdPartyApplicationConnectorMock.FetchSubscriptionsByUserId.willReturnSubscriptions(Seq.empty: _*)
      PrincipalThirdPartyApplicationConnectorMock.FetchSubscriptionsByUserId.willThrowException(new RuntimeException(expectedExceptionMessage))

      val ex = intercept[RuntimeException] {
        await(underTest.fetch(developer))
      }

      ex.getMessage shouldBe expectedExceptionMessage
    }
  }
}
