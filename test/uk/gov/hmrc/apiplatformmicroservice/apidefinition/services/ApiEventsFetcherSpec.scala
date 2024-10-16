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

package uk.gov.hmrc.apiplatformmicroservice.apidefinition.services

import scala.concurrent.ExecutionContext.Implicits.global

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.common.domain.models.Environment.{PRODUCTION, SANDBOX}
import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ServiceName
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.mocks.ApiDefinitionServiceModule
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.{ApiDefinitionTestDataHelper, ApiEventId, DisplayApiEvent}
import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec

class ApiEventsFetcherSpec extends AsyncHmrcSpec
    with ApiDefinitionTestDataHelper with FixedClock {

  trait Setup extends ApiDefinitionServiceModule {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val service                    = new EnvironmentAwareApiDefinitionService(SubordinateApiDefinitionServiceMock.aMock, PrincipalApiDefinitionServiceMock.aMock)
    val inTest                     = new ApiEventsFetcher(service)

    val serviceName      = ServiceName("someService")
    val displayApiEvent1 = DisplayApiEvent(ApiEventId.random, serviceName, instant, "Api Created", List.empty, None)
    val displayApiEvent2 = displayApiEvent1.copy(serviceName = ServiceName("hello-api-2"))
  }

  "fetchApiVersionsForEnvironment" should {

    "fetch api events for Principal environment" in new Setup() {
      PrincipalApiDefinitionServiceMock.FetchApiEvents.willReturn(List(displayApiEvent1))
      SubordinateApiDefinitionServiceMock.FetchApiEvents.willReturn(List(displayApiEvent2))

      val expectedApiEvent = displayApiEvent1.copy(environment = Some(PRODUCTION))

      await(inTest.fetchApiVersionsForEnvironment(PRODUCTION, serviceName)) should contain.only(expectedApiEvent)
    }

    "fetch api events for Subordinate environment" in new Setup() {
      PrincipalApiDefinitionServiceMock.FetchApiEvents.willReturn(List(displayApiEvent1))
      SubordinateApiDefinitionServiceMock.FetchApiEvents.willReturn(List(displayApiEvent2))

      val expectedApiEvent = displayApiEvent2.copy(environment = Some(SANDBOX))

      await(inTest.fetchApiVersionsForEnvironment(SANDBOX, serviceName)) should contain.only(expectedApiEvent)
    }

    "fetch api events, excluding no change events" in new Setup() {
      PrincipalApiDefinitionServiceMock.FetchApiEvents.willReturn(List(displayApiEvent1), includeNoChange = false)

      val expectedApiEvent = displayApiEvent1.copy(environment = Some(PRODUCTION))

      await(inTest.fetchApiVersionsForEnvironment(PRODUCTION, serviceName, includeNoChange = false)) should contain.only(expectedApiEvent)
    }

    "return empty list when no api events present in environment" in new Setup() {
      PrincipalApiDefinitionServiceMock.FetchApiEvents.willReturnEmptyList()
      SubordinateApiDefinitionServiceMock.FetchApiEvents.willReturn(List(displayApiEvent2))

      await(inTest.fetchApiVersionsForEnvironment(PRODUCTION, serviceName)) shouldBe List.empty
    }
  }
}
