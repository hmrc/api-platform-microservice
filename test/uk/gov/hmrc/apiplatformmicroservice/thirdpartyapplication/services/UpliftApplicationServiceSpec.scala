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

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.common.domain.models.{Environment, _}
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationWithCollaboratorsFixtures
import uk.gov.hmrc.apiplatform.modules.applications.core.interface.models.CreateApplicationRequestV2
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.mocks.ApiIdentifiersForUpliftFetcherModule
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiDefinitionTestDataHelper
import uk.gov.hmrc.apiplatformmicroservice.common.utils.{AsyncHmrcSpec, UpliftRequestSamples}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks.{ThirdPartyApplicationConnectorModule, _}

class UpliftApplicationServiceSpec extends AsyncHmrcSpec with ApplicationWithCollaboratorsFixtures with ApiDefinitionTestDataHelper with UpliftRequestSamples {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  trait Setup
      extends ApiIdentifiersForUpliftFetcherModule
      with ApplicationByIdFetcherModule
      with ThirdPartyApplicationConnectorModule
      with SubscriptionFieldsConnectorModule
      with SubscriptionFieldsServiceModule
      with SubscriptionServiceModule {

    val upliftService = new UpliftApplicationService(
      ApiIdentifiersForUpliftFetcherMock.aMock,
      PrincipalThirdPartyApplicationConnectorMock.aMock,
      ApplicationByIdFetcherMock.aMock,
      SubscriptionServiceMock.aMock
    )
  }

  "UpliftApplicationService" should {
    val sandboxApp   = standardApp.inSandbox()
    val newAppId     = ApplicationId.random
    val context1     = "context1".asIdentifier()
    val context2     = "context2".asIdentifier()
    val context3     = "context3".asIdentifier()
    val contextCDSv1 = "customs/declarations".asIdentifier("1.0".asVersion())
    val contextCDSv2 = "customs/declarations".asIdentifier("2.0".asVersion())

    val LEFT = Symbol("left")

    "successfully create an uplifted application" in new Setup {
      ApiIdentifiersForUpliftFetcherMock.FetchUpliftableApis.willReturn(context1, context2)
      PrincipalThirdPartyApplicationConnectorMock.CreateApplicationV2.willReturnSuccess(newAppId)
      ApplicationByIdFetcherMock.FetchApplication.willReturnApplication(Option(sandboxApp.modify(_.copy(deployedTo = Environment.PRODUCTION))))
      SubscriptionServiceMock.CreateManySubscriptionsForApplication.willReturnOk

      val result = await(upliftService.upliftApplicationV2(sandboxApp, Set(context1, context2), makeUpliftRequest(context1)))

      result shouldBe Right(newAppId)

      val createAppRequest = PrincipalThirdPartyApplicationConnectorMock.CreateApplicationV2.captureRequest()
      createAppRequest match {
        case v2: CreateApplicationRequestV2 => v2.upliftRequest.subscriptions shouldBe Set(context1)
        case _                              => fail("Not the expected request")
      }

      SubscriptionServiceMock.CreateManySubscriptionsForApplication.verifyCalled(Set(context1))
    }

    "successfully create an uplifted application AND handle CDS uplift" in new Setup {
      ApiIdentifiersForUpliftFetcherMock.FetchUpliftableApis.willReturn(context1, context2, contextCDSv1)
      PrincipalThirdPartyApplicationConnectorMock.CreateApplicationV2.willReturnSuccess(newAppId)
      ApplicationByIdFetcherMock.FetchApplication.willReturnApplication(Option(sandboxApp.modify(_.copy(deployedTo = Environment.PRODUCTION))))
      SubscriptionServiceMock.CreateManySubscriptionsForApplication.willReturnOk

      val result = await(upliftService.upliftApplicationV2(sandboxApp, Set(context1, context2, contextCDSv2), makeUpliftRequest(contextCDSv2)))

      result shouldBe Right(newAppId)

      val createAppRequest = PrincipalThirdPartyApplicationConnectorMock.CreateApplicationV2.captureRequest()
      createAppRequest match {
        case v2: CreateApplicationRequestV2 => v2.upliftRequest.subscriptions shouldBe Set(contextCDSv1)
        case _                              => fail("Not the expected request")
      }

      SubscriptionServiceMock.CreateManySubscriptionsForApplication.verifyCalled(Set(contextCDSv1))
    }

    "successfully handle inability to uplift application due to no upliftable subscriptions" in new Setup {
      ApiIdentifiersForUpliftFetcherMock.FetchUpliftableApis.willReturn(context1, context2)
      val result = await(upliftService.upliftApplicationV2(sandboxApp, Set(context1, context2), makeUpliftRequest(context3)))

      result shouldBe LEFT
    }

    "successfully handle when app is not a sandbox app" in new Setup {
      val applicationInProd = sandboxApp.modify(_.copy(deployedTo = Environment.PRODUCTION))
      val result            = await(upliftService.upliftApplicationV2(applicationInProd, Set(context1, context2), makeUpliftRequest(context3)))

      result shouldBe LEFT

      PrincipalThirdPartyApplicationConnectorMock.CreateApplicationV2.verifyNotCalled()
    }

    "ensure requested subscriptions are non empty" in new Setup {
      val result = await(upliftService.upliftApplicationV2(sandboxApp, Set(context1, context2), makeUpliftRequest()))
      result shouldBe LEFT

      PrincipalThirdPartyApplicationConnectorMock.CreateApplicationV2.verifyNotCalled()
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
