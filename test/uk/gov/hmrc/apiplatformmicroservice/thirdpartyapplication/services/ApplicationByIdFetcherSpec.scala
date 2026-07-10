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

import uk.gov.hmrc.apiplatform.modules.common.domain.models.*
import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.*
import uk.gov.hmrc.apiplatform.modules.applications.query.domain.models.ApplicationQuery
import uk.gov.hmrc.apiplatform.modules.subscriptionfields.domain.models.{ApiFieldMapFixtures, FieldNameFixtures, FieldValueFixtures, FieldsFixtures}
import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks.*

class ApplicationByIdFetcherSpec extends AsyncHmrcSpec
    with FixedClock
    with ApplicationWithCollaboratorsFixtures
    with ApiIdentifierFixtures
    with FieldNameFixtures
    with FieldValueFixtures
    with FieldsFixtures
    with ApiFieldMapFixtures {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val id: ApplicationId = standardApp.id

  val application: ApplicationWithCollaborators = standardApp.inSandbox()

  val BANG = new RuntimeException("BANG")

  trait Setup extends QueryConnectorMockModule
      with MockitoSugar
      with ArgumentMatchersSugar {

    val fetcher = new ApplicationByIdFetcher(
      QueryConnectorMock.aMock
    )
  }

  "ApplicationByIdFetcher" when {
    "fetchApplicationId is called" should {
      val qry = ApplicationQuery.ById(id, Nil)

      "return None if absent from principal and subordinate" in new Setup {
        QueryConnectorMock.ByQuery.returnsFor[Option[ApplicationWithCollaborators]](Environment.Sandbox, qry, None)
        QueryConnectorMock.ByQuery.returnsFor[Option[ApplicationWithCollaborators]](Environment.Production, qry, None)

        await(fetcher.fetchApplication(id)) shouldBe None
      }

      "return an application from subordinate if present" in new Setup {
        QueryConnectorMock.ByQuery.returnsFor[Option[ApplicationWithCollaborators]](Environment.Sandbox, qry, Some(application))
        QueryConnectorMock.ByQuery.returnsFor[Option[ApplicationWithCollaborators]](Environment.Production, qry, None)

        await(fetcher.fetchApplication(id)) shouldBe Some(application)
      }

      "return an application from principal if present" in new Setup {
        QueryConnectorMock.ByQuery.returnsFor[Option[ApplicationWithCollaborators]](Environment.Sandbox, qry, None)
        QueryConnectorMock.ByQuery.returnsFor[Option[ApplicationWithCollaborators]](Environment.Production, qry, Some(application))

        await(fetcher.fetchApplication(id)) shouldBe Some(application)
      }

      "return an application from principal if present even when subordinate throws" in new Setup {
        QueryConnectorMock.ByQuery.failsFor[Option[ApplicationWithCollaborators]](Environment.Sandbox, qry, BANG)
        QueryConnectorMock.ByQuery.returnsFor[Option[ApplicationWithCollaborators]](Environment.Production, qry, Some(application))

        await(fetcher.fetchApplication(id)) shouldBe Some(application)
      }

      "return an exception if principal throws even if subordinate has the application" in new Setup {
        QueryConnectorMock.ByQuery.returnsFor[Option[ApplicationWithCollaborators]](Environment.Sandbox, qry, Some(application))
        QueryConnectorMock.ByQuery.failsFor[Option[ApplicationWithCollaborators]](Environment.Production, qry, BANG)

        intercept[Exception] {
          await(fetcher.fetchApplication(id)) shouldBe Some(application)
        }.shouldBe(BANG)
      }
    }

    "fetchApplicationWithSubscriptionData" should {
      val qry = ApplicationQuery.ById(id, Nil, wantSubscriptions = true, wantSubscriptionFields = true)

      "return None when application is not found" in new Setup {
        QueryConnectorMock.ByQuery.returnsFor[Option[ApplicationWithSubscriptionFields]](Environment.Sandbox, qry, None)
        QueryConnectorMock.ByQuery.returnsFor[Option[ApplicationWithSubscriptionFields]](Environment.Production, qry, None)

        await(fetcher.fetchApplicationWithSubscriptionFields(id)) shouldBe None
      }

      "return an application with subscritions from subordinate if present" in new Setup {
        val subscriptions = Set(apiIdentifierOne)
        val subsFields    =
          Map(
            apiIdentifierOne.context -> Map(
              apiIdentifierOne.versionNbr -> fieldsMapOne
            )
          )

        QueryConnectorMock.ByQuery.returnsFor[Option[ApplicationWithSubscriptionFields]](
          Environment.Sandbox,
          qry,
          Some(application.withSubscriptions(subscriptions).withFieldValues(subsFields))
        )
        QueryConnectorMock.ByQuery.returnsFor[Option[ApplicationWithSubscriptionFields]](Environment.Production, qry, None)

        val expect = application.withSubscriptions(Set(apiIdentifierOne)).withFieldValues(subsFields)
        await(fetcher.fetchApplicationWithSubscriptionFields(id)) shouldBe Some(expect)
      }
    }
  }
}
