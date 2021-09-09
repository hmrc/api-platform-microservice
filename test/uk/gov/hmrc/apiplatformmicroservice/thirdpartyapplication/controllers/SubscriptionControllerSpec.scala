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

package uk.gov.hmrc.apiplatformmicroservice.apidefinition.controllers

import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiDefinitionTestDataHelper
import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks._

import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.ApplicationId
import play.api.libs.json.Json
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiContext
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiVersion
import play.api.test.Helpers.{contentAsJson, status}
import uk.gov.hmrc.apiplatformmicroservice.common.builder.ApplicationBuilder
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiIdentifier
import uk.gov.hmrc.apiplatformmicroservice.common.connectors.AuthConnector
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.controllers.SubscriptionController
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.UpliftApplicationService
import scala.concurrent.Future.successful
import akka.stream.testkit.NoMaterializer

class SubscriptionControllerSpec extends AsyncHmrcSpec with ApiDefinitionTestDataHelper {

  trait Setup extends ApplicationByIdFetcherModule with SubscriptionServiceModule with ApplicationBuilder {
    implicit val headerCarrier = HeaderCarrier()
    implicit val mat = NoMaterializer

    val applicationId = ApplicationId.random
    val context = ApiContext("hello")
    val version = ApiVersion("1.0")
    val apiIdentifier = ApiIdentifier(context, version)

    val apiId1 = "context1".asIdentifier()
    val apiId2 = "context2".asIdentifier()

    val mockAuthConfig = mock[AuthConnector.Config]
    val mockAuthConnector = mock[AuthConnector]
    val mockUpliftApplicationService = mock[UpliftApplicationService]

    val controller = new SubscriptionController(
      SubscriptionServiceMock.aMock,
      ApplicationByIdFetcherMock.aMock,
      mockAuthConfig,
      mockAuthConnector,
      Helpers.stubControllerComponents(),
      mockUpliftApplicationService
    )
  }

  "createSubscriptionForApplication" should {
    "return NO CONTENT when successfully subscribing to API" in new Setup() {
      val application = buildApplication(appId = applicationId)
      ApplicationByIdFetcherMock.FetchApplicationWithSubscriptionData.willReturnApplicationWithSubscriptionData(application)
      val request = FakeRequest("POST", s"/applications/${applicationId.value}/subscriptions")
      val payload = s"""{"context":"${context.value}", "version":"${version.value}"}"""

      SubscriptionServiceMock.CreateSubscriptionForApplication.willReturnSuccess
    
      val result = controller.subscribeToApi(applicationId, Some(false))(request.withBody(Json.parse(payload)))

      status(result) shouldBe NO_CONTENT
    }

    "return NOT FOUND when the application cannot subscribe to the API" in new Setup() {
      val application = buildApplication(appId = applicationId)
      ApplicationByIdFetcherMock.FetchApplicationWithSubscriptionData.willReturnApplicationWithSubscriptionData(application)
      val request = FakeRequest("POST", s"/applications/${applicationId.value}/subscriptions")
      val payload = s"""{"context":"${context.value}", "version":"${version.value}"}"""

      SubscriptionServiceMock.CreateSubscriptionForApplication.willReturnDenied

      val result = controller.subscribeToApi(applicationId, Some(false))(request.withBody(Json.parse(payload)))

      status(result) shouldBe NOT_FOUND
      contentAsJson(result) shouldBe Json.obj(
        "code" -> "APPLICATION_NOT_FOUND",
        "message" -> s"API $apiIdentifier is not available for application ${applicationId.value}"
      )
    }

    "return CONFLICT when the application is already subscribed to the API" in new Setup() {
      val application = buildApplication(appId = applicationId)
      ApplicationByIdFetcherMock.FetchApplicationWithSubscriptionData.willReturnApplicationWithSubscriptionData(application)
      val request = FakeRequest("POST", s"/applications/${applicationId.value}/subscriptions")
      val payload = s"""{"context":"${context.value}", "version":"${version.value}"}"""

      SubscriptionServiceMock.CreateSubscriptionForApplication.willReturnDuplicate

      val result = controller.subscribeToApi(applicationId, Some(false))(request.withBody(Json.parse(payload)))

      status(result) shouldBe CONFLICT
      contentAsJson(result) shouldBe Json.obj(
        "code" -> "SUBSCRIPTION_ALREADY_EXISTS",
        "message" -> s"Application: '${application.name}' is already Subscribed to API: ${context.value}: ${version.value}"
      )
    }
  }

  "fetchUpliftableSubscriptions" should {

    "return OK with a list of upliftable subscriptions" when {
      "there are upliftable apis available for the application id" in new Setup {
        val application = buildApplication(appId = applicationId)
        ApplicationByIdFetcherMock.FetchApplicationWithSubscriptionData.willReturnApplicationWithSubscriptionData(application, Set(apiId1, apiId2))
  
        val apiIdentifiers = Set(apiId1)
        when(mockUpliftApplicationService.fetchUpliftableApisForApplication(*)(*)).thenReturn(successful(apiIdentifiers))

        val result = controller.fetchUpliftableSubscriptions(ApplicationId.random)(FakeRequest())

        status(result) shouldBe OK
        contentAsJson(result) shouldBe Json.toJson(apiIdentifiers)
      }
    }

    "return NotFound" when {
      "there are no upliftable apis available for the application id" in new Setup {
        val application = buildApplication(appId = applicationId)
        ApplicationByIdFetcherMock.FetchApplicationWithSubscriptionData.willReturnApplicationWithSubscriptionData(application, Set(apiId1, apiId2))

        when(mockUpliftApplicationService.fetchUpliftableApisForApplication(*)(*)).thenReturn(successful(Set.empty[ApiIdentifier]))

        val result = controller.fetchUpliftableSubscriptions(ApplicationId.random)(FakeRequest())

        status(result) shouldBe NOT_FOUND
      }
    }
  }
}
