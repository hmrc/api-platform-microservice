/*
 * Copyright 2021 HM Revenue & Customs
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

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks.ThirdPartyApplicationConnectorModule
import org.mockito.MockitoSugar
import org.mockito.ArgumentMatchersSugar
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks._
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.mocks.ApiIdentifiersForUpliftFetcherModule
import uk.gov.hmrc.apiplatformmicroservice.common.builder.ApplicationBuilder
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.ApplicationId
import uk.gov.hmrc.http.BadRequestException
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiDefinitionTestDataHelper

class UpliftApplicationServiceSpec extends AsyncHmrcSpec with ApplicationBuilder with ApiDefinitionTestDataHelper {

  implicit val hc = HeaderCarrier()

  trait Setup extends ApiIdentifiersForUpliftFetcherModule with ThirdPartyApplicationConnectorModule with SubscriptionFieldsConnectorModule with SubscriptionFieldsFetcherModule with MockitoSugar with ArgumentMatchersSugar {
    val fetcher = new ApplicationByIdFetcher(EnvironmentAwareThirdPartyApplicationConnectorMock.instance, EnvironmentAwareSubscriptionFieldsConnectorMock.instance, SubscriptionFieldsFetcherMock.aMock)

    val upliftService = new UpliftApplicationService(ApiIdentifiersForUpliftFetcherMock.aMock, PrincipalThirdPartyApplicationConnectorMock.aMock)
  }
  
  "UpliftApplicationService" should {
    val applicationId = ApplicationId.random
    val application = buildApplication(appId = applicationId)
    val newAppId = ApplicationId.random

    "succesfully create an uplifted application" in new Setup {
      ApiIdentifiersForUpliftFetcherMock.UpliftApplication.willReturnApiDefinitions("context1".asIdentifier(), "context2".asIdentifier())
      PrincipalThirdPartyApplicationConnectorMock.CreateApplication.willReturnSuccess(newAppId)
      await(upliftService.upliftApplication(application, Set("context1".asIdentifier)))
    }

    "fails with Bad Request exception when trying to uplift an application with zero subscriptions" in new Setup {
      ApiIdentifiersForUpliftFetcherMock.UpliftApplication.willReturnApiDefinitions("context1".asIdentifier(), "context2".asIdentifier())
      intercept[BadRequestException] {
        await(upliftService.upliftApplication(application, Set.empty))
      }.message shouldBe s"No subscriptions for uplift of application with id: ${applicationId.value}"
    }

    "fails with Bad Request exception when trying to uplift an application with zero subscriptions than can be uplifted" in new Setup {
      ApiIdentifiersForUpliftFetcherMock.UpliftApplication.willReturnApiDefinitions("context1".asIdentifier(), "context2".asIdentifier())
      intercept[BadRequestException] {
        await(upliftService.upliftApplication(application, Set("context3".asIdentifier)))
      }.message shouldBe s"No subscriptions for uplift of application with id: ${applicationId.value}"              
    }
  }
}