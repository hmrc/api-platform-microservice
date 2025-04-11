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
import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models._
import uk.gov.hmrc.apiplatform.modules.applications.subscriptions.domain.models.{ApiFieldMapFixtures, FieldNameFixtures, FieldValueFixtures, FieldsFixtures}
import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks._

class ApplicationByIdFetcherSpec extends AsyncHmrcSpec
    with FixedClock
    with ApplicationWithCollaboratorsFixtures
    with ApiIdentifierFixtures
    with FieldNameFixtures
    with FieldValueFixtures
    with FieldsFixtures
    with ApiFieldMapFixtures {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val id: ApplicationId = standardApp.id // ApplicationId.random

  val application: ApplicationWithCollaborators = standardApp.inSandbox()

  val BANG = new RuntimeException("BANG")

  trait Setup extends ThirdPartyApplicationConnectorModule with SubscriptionFieldsConnectorModule with SubscriptionFieldsServiceModule with MockitoSugar
      with ArgumentMatchersSugar {

    val fetcher = new ApplicationByIdFetcher(
      EnvironmentAwareThirdPartyApplicationConnectorMock.instance,
      EnvironmentAwareSubscriptionFieldsConnectorMock.instance,
      SubscriptionFieldsServiceMock.aMock
    )
  }

  "ApplicationByIdFetcher" when {
    "fetchApplicationId is called" should {
      "return None if absent from principal and subordinate" in new Setup {
        EnvironmentAwareThirdPartyApplicationConnectorMock.Subordinate.FetchApplicationById.willReturnNone
        EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.FetchApplicationById.willReturnNone

        await(fetcher.fetchApplication(id)) shouldBe None
      }

      "return an application from subordinate if present" in new Setup {
        EnvironmentAwareThirdPartyApplicationConnectorMock.Subordinate.FetchApplicationById.willReturnApplication(application)
        EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.FetchApplicationById.willReturnNone

        await(fetcher.fetchApplication(id)) shouldBe Some(application)
      }

      "return an application from principal if present" in new Setup {
        EnvironmentAwareThirdPartyApplicationConnectorMock.Subordinate.FetchApplicationById.willReturnNone
        EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.FetchApplicationById.willReturnApplication(application)
        await(fetcher.fetchApplication(id)) shouldBe Some(application)
      }

      "return an application from principal if present even when subordinate throws" in new Setup {
        EnvironmentAwareThirdPartyApplicationConnectorMock.Subordinate.FetchApplicationById.willThrowException(BANG)
        EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.FetchApplicationById.willReturnApplication(application)
        await(fetcher.fetchApplication(id)) shouldBe Some(application)
      }

      "return an exception if principal throws even if subordinate has the application" in new Setup {
        EnvironmentAwareThirdPartyApplicationConnectorMock.Subordinate.FetchApplicationById.willReturnApplication(application)
        EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.FetchApplicationById.willThrowException(BANG)
        intercept[Exception] {
          await(fetcher.fetchApplication(id)) shouldBe Some(application)
        }.shouldBe(BANG)
      }
    }

    "fetchApplicationWithSubscriptionData" should {

      "return None when application is not found" in new Setup {
        EnvironmentAwareThirdPartyApplicationConnectorMock.Subordinate.FetchApplicationById.willReturnNone
        EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.FetchApplicationById.willReturnNone

        await(fetcher.fetchApplicationWithSubscriptionData(id)) shouldBe None
      }

      "return an application with subscritions from subordinate if present" in new Setup {
        val subsFields =
          Map(
            apiIdentifierOne.context -> Map(
              apiIdentifierOne.versionNbr -> fieldsMapOne
            )
          )

        EnvironmentAwareThirdPartyApplicationConnectorMock.Subordinate.FetchApplicationById.willReturnApplication(application)
        EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.FetchApplicationById.willReturnNone
        EnvironmentAwareThirdPartyApplicationConnectorMock.Subordinate.FetchSubscriptionsById.willReturnSubscriptions(apiIdentifierOne)
        SubscriptionFieldsServiceMock.FetchFieldValuesWithDefaults.willReturnFieldValues(subsFields)

        val expect = application.withSubscriptions(Set(apiIdentifierOne)).withFieldValues(subsFields)
        await(fetcher.fetchApplicationWithSubscriptionData(id)) shouldBe Some(expect)
      }
    }
  }
}
