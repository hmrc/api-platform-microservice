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
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks._
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.mocks.ApiIdentifiersForUpliftFetcherModule
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.{ApiDefinitionTestDataHelper, ApiIdentifier}
import uk.gov.hmrc.apiplatformmicroservice.common.builder.ApplicationBuilder
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiDefinitionTestDataHelper
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.Environment

class UpliftApplicationServiceSpec extends AsyncHmrcSpec with ApplicationBuilder with ApiDefinitionTestDataHelper {

  implicit val hc = HeaderCarrier()

  trait Setup extends ApiIdentifiersForUpliftFetcherModule with ApplicationByIdFetcherModule with ThirdPartyApplicationConnectorModule with SubscriptionFieldsConnectorModule with SubscriptionFieldsFetcherModule {
    val fetcher = new ApplicationByIdFetcher(EnvironmentAwareThirdPartyApplicationConnectorMock.instance, EnvironmentAwareSubscriptionFieldsConnectorMock.instance, SubscriptionFieldsFetcherMock.aMock)

    val upliftService = new UpliftApplicationService(ApiIdentifiersForUpliftFetcherMock.aMock, PrincipalThirdPartyApplicationConnectorMock.aMock, fetcher)
  }
  
  "UpliftApplicationService" should {
    val applicationId = ApplicationId.random
    val application = buildApplication(appId = applicationId)
    val newAppId = ApplicationId.random
    val context1 = "context1".asIdentifier
    val context2 = "context2".asIdentifier()
    val context3 = "context3".asIdentifier()
    val contextCDSv1 = "customs/declarations".asIdentifier("1.0".asVersion)
    val contextCDSv2 = "customs/declarations".asIdentifier("2.0".asVersion)

    "successfully create an uplifted application" in new Setup {
      ApiIdentifiersForUpliftFetcherMock.FetchUpliftableApis.willReturn(context1, context2)
      PrincipalThirdPartyApplicationConnectorMock.CreateApplication.willReturnSuccess(newAppId)

      val result = await(upliftService.upliftApplication(application, Set(context1, context2), Set(context1)))

      result shouldBe Right(newAppId)

      val createAppRequest = PrincipalThirdPartyApplicationConnectorMock.CreateApplication.captureRequest
      createAppRequest.subscriptions shouldBe Set(context1)
    }

    "successfully create an uplifted application AND handle CDS uplift" in new Setup {
      ApiIdentifiersForUpliftFetcherMock.FetchUpliftableApis.willReturn(context1, context2, contextCDSv1)
      PrincipalThirdPartyApplicationConnectorMock.CreateApplication.willReturnSuccess(newAppId)

      val result = await(upliftService.upliftApplication(application, Set(context1, context2, contextCDSv2), Set(contextCDSv2)))

      result shouldBe Right(newAppId)

      val createAppRequest = PrincipalThirdPartyApplicationConnectorMock.CreateApplication.captureRequest
      createAppRequest.subscriptions shouldBe Set(contextCDSv1)
    }

    "successfully handle inability to uplift application due to no upliftable subscriptions" in new Setup {
      ApiIdentifiersForUpliftFetcherMock.FetchUpliftableApis.willReturn(context1, context2)
      val result = await(upliftService.upliftApplication(application, Set(context1, context2), Set(context3)))

      result shouldBe ('left)
    }

    "successfully handle when app is not a sandbox app" in new Setup {
      val applicationInProd = application.copy(deployedTo = Environment.PRODUCTION)
      val result = await(upliftService.upliftApplication(applicationInProd, Set(context1, context2), Set(context3)))

      result shouldBe ('left)

      PrincipalThirdPartyApplicationConnectorMock.CreateApplication.verifyNotCalled
    }
    
    "ensure requested subscriptions are non empty" in new Setup {
      intercept[IllegalArgumentException]{
        await(upliftService.upliftApplication(application, Set(context1, context2), Set()))
      }
      PrincipalThirdPartyApplicationConnectorMock.CreateApplication.verifyNotCalled
    }


    "returns a set of upliftable apis for an application" when {
      "upliftable apis are available" in new Setup {
        val subscriptions = Set(context1, context2, context3)
        ApiIdentifiersForUpliftFetcherMock.FetchUpliftableApis.willReturn(context1, context2)

        await(upliftService.fetchUpliftableApisForApplication(subscriptions)) shouldBe Set(context1, context2)
      }
    }

    "returns an empty set for an application" when {
      "non upliftable apis are removed and upliftable apis are not available" in new Setup {
        val subscriptions = Set(context1, context2, context3)
        ApiIdentifiersForUpliftFetcherMock.FetchUpliftableApis.willReturn()

        await(upliftService.fetchUpliftableApisForApplication(subscriptions)) shouldBe Set.empty[ApiIdentifier]
      }
    }
  }
}
