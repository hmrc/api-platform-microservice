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

class SubscriptionFieldsControllerSpec
    extends AsyncHmrcSpec
    with ApiDefinitionTestDataHelper
    with ClockNow
    with ApplicationIdFixtures
    with ApiIdentifierFixtures {

  val clock = Clock.systemUTC()

  trait Setup extends ApplicationByIdFetcherModule with SubscriptionServiceModule with ApplicationWithCollaboratorsFixtures with SubscriptionFieldsConnectorModule {
    implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
    implicit val mat: Materializer            = NoMaterializer

    val mockAuthConfig               = mock[AuthConnector.Config]
    val mockAuthConnector            = mock[AuthConnector]
    val mockUpliftApplicationService = mock[UpliftApplicationService]

    val controller = new SubscriptionFieldsController(
      Helpers.stubControllerComponents(),
      EnvironmentAwareSubscriptionFieldsConnectorMock.instance
    )
  }

  "upsertSubscriptionFields" should {
    "successfully call the connector and receive an HTTP 200 response" in new Setup {
      PrincipalSubscriptionFieldsConnectorMock.UpsertFieldValues.willReturn(apiIdentifierOne)(OK)

      val result = controller.upsertSubscriptionFields(
        standardApp.details.deployedTo,
        standardApp.details.clientId,
        apiIdentifierOne.context,
        apiIdentifierOne.versionNbr
      )(
        FakeRequest().withBody(
          Json.parse("{}")
        )
      )

      status(result) shouldBe OK
      contentAsJson(result) shouldBe Json.toJson("")
    }
  }
}
