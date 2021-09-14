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

import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors._
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.{ApplicationId, Environment}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.Application
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors._
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks._
import org.mockito.MockitoSugar
import org.mockito.ArgumentMatchersSugar
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.ApplicationWithSubscriptionData
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.ClientId
import uk.gov.hmrc.time.DateTimeUtils
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.mocks.ApiDefinitionServiceModule
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiDefinitionTestDataHelper
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiVersion

class ApplicationByIdFetcherSpec extends AsyncHmrcSpec with ApiDefinitionTestDataHelper {

  implicit val hc = HeaderCarrier()

  val id: ApplicationId = ApplicationId.random
  val clientId: ClientId = ClientId("123")
  val application: Application = Application(id, clientId, "gatewayId", "name", DateTimeUtils.now, DateTimeUtils.now, None, Environment.SANDBOX, Some("description"))
  val BANG = new RuntimeException("BANG")

  trait Setup 
  extends ThirdPartyApplicationConnectorModule 
  with SubscriptionFieldsConnectorModule 
  with SubscriptionFieldsFetcherModule
  with ApiDefinitionServiceModule
  with MockitoSugar 
  with ArgumentMatchersSugar {
    val fetcher = new ApplicationByIdFetcher(EnvironmentAwareThirdPartyApplicationConnectorMock.instance, EnvironmentAwareSubscriptionFieldsConnectorMock.instance, SubscriptionFieldsFetcherMock.aMock, EnvironmentAwareApiDefinitionServiceMock.instance)
  }

  val apiAversion1 = apiVersion().asRetired
  val apiAversion2 = apiVersion(ApiVersion("2.0"))
  val apiA = apiDefinition("A").withVersions(apiAversion1, apiAversion2)
  val apiBversion1 = apiVersion()
  val apiBversion2 = apiVersion()
  val apiB = apiDefinition("B").withVersions(apiBversion1, apiBversion2)
  val apiCversion1 = apiVersion().asRetired
  val apiC = apiDefinition("C").withVersions(apiCversion1)

  "ApplicationByIdFetcher" when {
    "fetchApplicationId is called" should {
      "return None if absent from principal and subordinate" in new Setup {
        EnvironmentAwareThirdPartyApplicationConnectorMock.Subordinate.FetchApplicationById.willReturnNone
        EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.FetchApplicationById.willReturnNone
        EnvironmentAwareApiDefinitionServiceMock.Subordinate.FetchAllApiDefinitions.willReturnNoApiDefinitions()

        await(fetcher.fetchApplication(id)) shouldBe None
      }

      "return an application from subordinate if present" in new Setup {
        EnvironmentAwareThirdPartyApplicationConnectorMock.Subordinate.FetchApplicationById.willReturnApplication(application)
        EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.FetchApplicationById.willReturnNone
        EnvironmentAwareApiDefinitionServiceMock.Subordinate.FetchAllApiDefinitions.willReturnNoApiDefinitions()
        await(fetcher.fetchApplication(id)) shouldBe Some(application)
      }

      "return an application from principal if present" in new Setup {
        EnvironmentAwareThirdPartyApplicationConnectorMock.Subordinate.FetchApplicationById.willReturnNone
        EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.FetchApplicationById.willReturnApplication(application)
        EnvironmentAwareApiDefinitionServiceMock.Subordinate.FetchAllApiDefinitions.willReturnNoApiDefinitions()

        await(fetcher.fetchApplication(id)) shouldBe Some(application)
      }

      "return an application from principal if present even when subordinate throws" in new Setup {
        EnvironmentAwareThirdPartyApplicationConnectorMock.Subordinate.FetchApplicationById.willThrowException(BANG)
        EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.FetchApplicationById.willReturnApplication(application)
        EnvironmentAwareApiDefinitionServiceMock.Subordinate.FetchAllApiDefinitions.willReturnNoApiDefinitions()

        await(fetcher.fetchApplication(id)) shouldBe Some(application)
      }

      "return an exception if principal throws even if subordinate has the application" in new Setup {
        EnvironmentAwareThirdPartyApplicationConnectorMock.Subordinate.FetchApplicationById.willReturnApplication(application)
        EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.FetchApplicationById.willThrowException(BANG)
        EnvironmentAwareApiDefinitionServiceMock.Subordinate.FetchAllApiDefinitions.willReturnNoApiDefinitions()

        intercept[Exception] {
          await(fetcher.fetchApplication(id)) shouldBe Some(application)
        }.shouldBe(BANG)
      }
    }

    "fetchApplicationWithSubscriptionData" should {
      import SubscriptionsHelper._

      "return None when application is not found" in new Setup {
        EnvironmentAwareThirdPartyApplicationConnectorMock.Subordinate.FetchApplicationById.willReturnNone
        EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.FetchApplicationById.willReturnNone
        EnvironmentAwareApiDefinitionServiceMock.Subordinate.FetchAllApiDefinitions.willReturnNoApiDefinitions()

        await(fetcher.fetchApplicationWithSubscriptionData(id)) shouldBe None
      }

      "return an application with subscriptions from subordinate if present" in new Setup {
        val fieldsForAOne = FieldNameOne -> "oneValue".asFieldValue
        val fieldsForATwo = FieldNameTwo -> "twoValue".asFieldValue

        val subsFields =
          Map(
            ContextA -> Map(
              VersionOne -> Map(fieldsForAOne),
              VersionTwo -> Map(fieldsForATwo)
            )
          )

        EnvironmentAwareThirdPartyApplicationConnectorMock.Subordinate.FetchApplicationById.willReturnApplication(application)
        EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.FetchApplicationById.willReturnNone
        EnvironmentAwareThirdPartyApplicationConnectorMock.Subordinate.FetchSubscriptionsById.willReturnSubscriptions(ApiIdentifierAOne)
        SubscriptionFieldsFetcherMock.fetchFieldValuesWithDefaults.willReturnFieldValues(subsFields)
        EnvironmentAwareApiDefinitionServiceMock.Subordinate.FetchAllApiDefinitions.willReturnNoApiDefinitions()

        val expect = ApplicationWithSubscriptionData(
          application,
          Set(ApiIdentifierAOne),
          subsFields
        )
        await(fetcher.fetchApplicationWithSubscriptionData(id)) shouldBe Some(expect)
      }

      "return an application with subscriptions from subordinate ignoring retired apis" in new Setup {
        EnvironmentAwareThirdPartyApplicationConnectorMock.Subordinate.FetchApplicationById.willReturnApplication(application)
        EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.FetchApplicationById.willReturnNone
        EnvironmentAwareApiDefinitionServiceMock.Subordinate.FetchAllApiDefinitions.willReturn(apiA, apiB, apiC)
        SubscriptionFieldsFetcherMock.fetchFieldValuesWithDefaults.willReturnFieldValues(Map.empty)
        EnvironmentAwareThirdPartyApplicationConnectorMock.Subordinate.FetchSubscriptionsById.willReturnSubscriptions(ApiIdentifierAOne, ApiIdentifierATwo, ApiIdentifierBTwo)

        // A-1 is retired, C-1 is retired
        val expect = ApplicationWithSubscriptionData(
          application,
          Set(ApiIdentifierATwo, ApiIdentifierBTwo),
          Map.empty
        )
        await(fetcher.fetchApplicationWithSubscriptionData(id)) shouldBe Some(expect)
      }
    }
  }
}
