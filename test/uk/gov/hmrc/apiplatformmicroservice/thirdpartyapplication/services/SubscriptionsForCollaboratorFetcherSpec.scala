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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services

import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiDefinitionTestDataHelper
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks.ThirdPartyApplicationConnectorModule
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.models.APIIdentifier
import uk.gov.hmrc.apiplatformmicroservice.util.AsyncHmrcSpec
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class SubscriptionsForCollaboratorFetcherSpec extends AsyncHmrcSpec with ApiDefinitionTestDataHelper {

  trait Setup extends ThirdPartyApplicationConnectorModule {
    implicit val headerCarrier = HeaderCarrier()
    val email = "joebloggs@example.com"
    val subordinateSubscriptions = Seq(APIIdentifier("hello-world", "1.0"), APIIdentifier("hello-world", "2.0"))
    val principalSubscriptions = Seq(APIIdentifier("hello-world", "1.0"), APIIdentifier("hello-agents", "1.0"))
    val underTest = new SubscriptionsForCollaboratorFetcher(SubordinateThirdPartyApplicationConnectorMock.aMock,
      PrincipalThirdPartyApplicationConnectorMock.aMock)
  }

  "SubscriptionsForCollaboratorFetcher" should {
    "concatenate both subordinate and principal subscriptions without duplicates" in new Setup {
      SubordinateThirdPartyApplicationConnectorMock.FetchSubscriptionsByEmail.willReturnSubscriptions(subordinateSubscriptions: _*)
      PrincipalThirdPartyApplicationConnectorMock.FetchSubscriptionsByEmail.willReturnSubscriptions(principalSubscriptions: _*)

      val result = await(underTest(email))

      result shouldBe Set(APIIdentifier("hello-world", "1.0"), APIIdentifier("hello-world", "2.0"), APIIdentifier("hello-agents", "1.0"))
    }

    "return subordinate subscriptions if there are no matching principal subscriptions" in new Setup {
      SubordinateThirdPartyApplicationConnectorMock.FetchSubscriptionsByEmail.willReturnSubscriptions(subordinateSubscriptions: _*)
      PrincipalThirdPartyApplicationConnectorMock.FetchSubscriptionsByEmail.willReturnSubscriptions(Seq.empty: _*)

      val result = await(underTest(email))

      result should contain theSameElementsAs subordinateSubscriptions
    }

    "return principal subscriptions if there are no matching subordinate subscriptions" in new Setup {
      SubordinateThirdPartyApplicationConnectorMock.FetchSubscriptionsByEmail.willReturnSubscriptions(Seq.empty: _*)
      PrincipalThirdPartyApplicationConnectorMock.FetchSubscriptionsByEmail.willReturnSubscriptions(principalSubscriptions: _*)

      val result = await(underTest(email))

      result should contain theSameElementsAs principalSubscriptions
    }

    "return an empty set if there are no matching subscriptions in any environment" in new Setup {
      SubordinateThirdPartyApplicationConnectorMock.FetchSubscriptionsByEmail.willReturnSubscriptions(Seq.empty: _*)
      PrincipalThirdPartyApplicationConnectorMock.FetchSubscriptionsByEmail.willReturnSubscriptions(Seq.empty: _*)

      val result = await(underTest(email))

      result shouldBe Set.empty
    }

    "return principal subscriptions if something goes wrong in subordinate" in new Setup {
      val expectedExceptionMessage = "something went wrong"
      SubordinateThirdPartyApplicationConnectorMock.FetchSubscriptionsByEmail.willThrowException(new RuntimeException(expectedExceptionMessage))
      PrincipalThirdPartyApplicationConnectorMock.FetchSubscriptionsByEmail.willReturnSubscriptions(principalSubscriptions: _*)

      val result = await(underTest(email))

      result should contain theSameElementsAs principalSubscriptions
    }

    "throw exception if something goes wrong in principal" in new Setup {
      val expectedExceptionMessage = "something went wrong"
      SubordinateThirdPartyApplicationConnectorMock.FetchSubscriptionsByEmail.willReturnSubscriptions(Seq.empty: _*)
      PrincipalThirdPartyApplicationConnectorMock.FetchSubscriptionsByEmail.willThrowException(new RuntimeException(expectedExceptionMessage))

      val ex = intercept[RuntimeException] {
        await(underTest(email))
      }

      ex.getMessage shouldBe expectedExceptionMessage
    }
  }
}