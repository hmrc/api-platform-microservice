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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services

import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.services.ApiDefinitionsForApplicationFetcher

import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiIdentifier

import scala.concurrent.Future.successful
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiDefinitionTestDataHelper
import uk.gov.hmrc.apiplatformmicroservice.common.builder.ApplicationBuilder
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiVersion
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.SubscriptionService.CreateSubscriptionDuplicate
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.SubscriptionService.CreateSubscriptionDenied
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.SubscriptionService.CreateSubscriptionSuccess
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks.ThirdPartyApplicationConnectorModule
import org.mockito.MockitoSugar
import org.mockito.ArgumentMatchersSugar
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks.SubscriptionFieldsFetcherModule
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks.SubscriptionFieldsConnectorModule

class SubscriptionServiceSpec extends AsyncHmrcSpec {
  trait Setup 
      extends ApplicationBuilder
      with ApiDefinitionTestDataHelper
      with ThirdPartyApplicationConnectorModule
      with SubscriptionFieldsFetcherModule
      with SubscriptionFieldsConnectorModule
      with MockitoSugar
      with ArgumentMatchersSugar {
        
    val mockApiDefinitionsForApplicationFetcher = mock[ApiDefinitionsForApplicationFetcher]
    val underTest = new SubscriptionService(
      mockApiDefinitionsForApplicationFetcher,
      EnvironmentAwareThirdPartyApplicationConnectorMock.instance,
      EnvironmentAwareSubscriptionFieldsConnectorMock.instance,
      SubscriptionFieldsFetcherMock.aMock
    )

    implicit val hc = HeaderCarrier()
    
    val apiDefinitionOne = apiDefinition("one")
    val apiDefinitionTwo = apiDefinition("two")
    val apiDefinitionThree = apiDefinition("three")
    val apiDefintions = Seq(apiDefinitionOne, apiDefinitionTwo, apiDefinitionThree)
    when(mockApiDefinitionsForApplicationFetcher.fetch(*, *, *)(*)).thenReturn(successful(apiDefintions.toList))

    val apiVersionOne = ApiVersion("1.0")
    val apiVersionTwo = ApiVersion("2.0")
    val apiIdentifierOne = ApiIdentifier(apiDefinitionOne.context, apiVersionOne)
    val apiIdentifierTwo = ApiIdentifier(apiDefinitionTwo.context, apiVersionOne)
    val apiIdentifierThree = ApiIdentifier(apiDefinitionThree.context, apiVersionOne)

    val applicationId = ApplicationId.random
    val application = buildApplication(appId = applicationId)
  }

  "createSubscriptionForApplication" should {
    "CreateSubscriptionDuplicate when application is already subscribed to the API " in new Setup {
      val duplicateApi = apiIdentifierOne
      val existingApiSubscriptions = Set(apiIdentifierOne, apiIdentifierTwo)

      val result = await(underTest.createSubscriptionForApplication(application, existingApiSubscriptions, duplicateApi, false))

      result shouldBe CreateSubscriptionDuplicate
    }

    "CreateSubscriptionDenied when the application cannot subscribe to the API " in new Setup {
      val deniedApi = ApiIdentifier(apiDefinitionOne.context, apiVersionTwo)
      val existingApiSubscriptions = Set(apiIdentifierOne, apiIdentifierTwo)

      val result = await(underTest.createSubscriptionForApplication(application, existingApiSubscriptions, deniedApi, false))

      result shouldBe CreateSubscriptionDenied
    }

    "CreateSubscriptionSuccess when successfully subscribing to API " in new Setup {
      val goodApi = apiIdentifierThree
      val existingApiSubscriptions = Set(apiIdentifierOne, apiIdentifierTwo)

      EnvironmentAwareThirdPartyApplicationConnectorMock.Subordinate.SubscribeToApi.willReturnSuccess

      val result = await(underTest.createSubscriptionForApplication(application, existingApiSubscriptions, goodApi, false))

      result shouldBe CreateSubscriptionSuccess
    }
  }
}
