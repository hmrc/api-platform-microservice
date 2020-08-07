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

import uk.gov.hmrc.apiplatformmicroservice.util.AsyncHmrcSpec
import uk.gov.hmrc.http.HeaderCarrier
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.EnvironmentAwareSubscriptionFieldsConnector
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.Application
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors._
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks.ThirdPartyApplicationConnectorModule
import org.mockito.MockitoSugar
import org.mockito.ArgumentMatchersSugar

class ApplicationByIdFetcherSpec extends AsyncHmrcSpec {

  trait Setup extends ThirdPartyApplicationConnectorModule with MockitoSugar with ArgumentMatchersSugar {

    implicit val hc = HeaderCarrier()

    val id: ApplicationId = ApplicationId("one")
    val application: Application = mock[Application]
    val BANG = new RuntimeException("BANG")
    val underTest = new ApplicationByIdFetcher(EnvironmentAwareThirdPartyApplicationConnectorMock.instance, mock[EnvironmentAwareSubscriptionFieldsConnector])
  }

  "ApplicationByIdFetcher" when {
    "fetchApplicationId is called" should {
      "return None if absent" in new Setup {
        EnvironmentAwareThirdPartyApplicationConnectorMock.Subordinate.FetchApplicationById.willReturnNone
        EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.FetchApplicationById.willReturnNone

        await(underTest.fetchApplication(id)) shouldBe None
      }

      "return an application from subordinate if present" in new Setup {
        EnvironmentAwareThirdPartyApplicationConnectorMock.Subordinate.FetchApplicationById.willReturnNone
        EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.FetchApplicationById.willReturnApplication(application)

        await(underTest.fetchApplication(id)) shouldBe Some(application)
      }

      "return an application from principal if present" in new Setup {
        EnvironmentAwareThirdPartyApplicationConnectorMock.Subordinate.FetchApplicationById.willReturnNone
        EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.FetchApplicationById.willReturnApplication(application)
        await(underTest.fetchApplication(id)) shouldBe Some(application)
      }

      "return an application from principal if present even when subordinate throws" in new Setup {
        EnvironmentAwareThirdPartyApplicationConnectorMock.Subordinate.FetchApplicationById.willThrowException(BANG)
        EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.FetchApplicationById.willReturnApplication(application)
        await(underTest.fetchApplication(id)) shouldBe Some(application)
      }

      "return an exception if principal throws even if subordinate has the application" in new Setup {
        EnvironmentAwareThirdPartyApplicationConnectorMock.Subordinate.FetchApplicationById.willReturnApplication(application)
        EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.FetchApplicationById.willThrowException(BANG)
        intercept[Exception] {
          await(underTest.fetchApplication(id)) shouldBe Some(application)
        }.shouldBe(BANG)
      }
    }
  }
}
