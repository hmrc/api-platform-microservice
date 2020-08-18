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
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors._
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.Application
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors._
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks._
import org.mockito.MockitoSugar
import org.mockito.ArgumentMatchersSugar
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.ApplicationWithSubscriptionData
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.Environment
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.ClientId
import uk.gov.hmrc.time.DateTimeUtils

class ApplicationByIdFetcherSpec extends AsyncHmrcSpec {

  implicit val hc = HeaderCarrier()

  val id: ApplicationId = ApplicationId("one")
  val clientId: ClientId = ClientId("123")
  val application: Application = Application(id, clientId, "name", DateTimeUtils.now, DateTimeUtils.now, None, Environment.SANDBOX, Some("description"))
  val BANG = new RuntimeException("BANG")

  trait Setup extends ThirdPartyApplicationConnectorModule with SubscriptionFieldsConnectorModule with MockitoSugar with ArgumentMatchersSugar {

    val fetcher = new ApplicationByIdFetcher(EnvironmentAwareThirdPartyApplicationConnectorMock.instance, EnvironmentAwareSubscriptionFieldsConnectorMock.instance)
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
      import SubscriptionsHelper._

      // def fetchApplicationWithSubscriptionData(id: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[ApplicationWithSubscriptionData]] = {

      "return None when application is not found" in new Setup {
        EnvironmentAwareThirdPartyApplicationConnectorMock.Subordinate.FetchApplicationById.willReturnNone
        EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.FetchApplicationById.willReturnNone

        await(fetcher.fetchApplicationWithSubscriptionData(id)) shouldBe None
      }

      "return an application with subscritions from subordinate if present" in new Setup {
        val fieldsForAOne = FieldNameOne -> "oneValue".asFieldValue
        val fieldsForATwo = FieldNameTwo -> "twoValue".asFieldValue

        val subsFields =
          Map(ContextA -> Map(
            VersionOne -> Map(fieldsForAOne),
            VersionTwo -> Map(fieldsForATwo)
          ))

        EnvironmentAwareThirdPartyApplicationConnectorMock.Subordinate.FetchApplicationById.willReturnApplication(application)
        EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.FetchApplicationById.willReturnNone
        EnvironmentAwareThirdPartyApplicationConnectorMock.Subordinate.FetchSubscriptionsById.willReturnSubscriptions(ApiIdentifierAOne)
        EnvironmentAwareSubscriptionFieldsConnectorMock.Subordinate.BulkFetchFieldValues.willReturnFields(subsFields)

        val expect = ApplicationWithSubscriptionData(
          application,
          Set(ApiIdentifierAOne),
          subsFields
        )
        await(fetcher.fetchApplicationWithSubscriptionData(id)) shouldBe Some(expect)
      }
    }
  }
}
