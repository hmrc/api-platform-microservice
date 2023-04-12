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
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.reflect.ClassTag

import akka.Done

import play.api.cache.AsyncCacheApi
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.developers.domain.models.UserId
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.mocks.ApiDefinitionServiceModule
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiStatus.{BETA, RETIRED, STABLE}
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models._
import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks.{ApplicationIdsForCollaboratorFetcherModule, SubscriptionsForCollaboratorFetcherModule}

class ExtendedApiDefinitionForCollaboratorFetcherSpec extends AsyncHmrcSpec with ApiDefinitionTestDataHelper {

  private val versionOne = ApiVersion("1.0")
  private val versionTwo = ApiVersion("2.0")

  val doNothingCache = new AsyncCacheApi {
    def set(key: String, value: Any, expiration: Duration = Duration.Inf): Future[Done] = Future.successful(Done)
    def remove(key: String): Future[Done]                                               = Future.successful(Done)

    def getOrElseUpdate[A: ClassTag](key: String, expiration: Duration = Duration.Inf)(orElse: => Future[A]): Future[A] = orElse

    def get[T: ClassTag](key: String): Future[Option[T]] = Future.successful(None)

    def removeAll(): Future[Done] = Future.successful(Done)
  }

  trait Setup extends ApiDefinitionServiceModule with ApplicationIdsForCollaboratorFetcherModule with SubscriptionsForCollaboratorFetcherModule {
    implicit val headerCarrier     = HeaderCarrier()
    val email                      = Some(UserId.random)
    val applicationId              = ApplicationId.random
    val helloApiDefinition         = apiDefinition("hello-api")
    val requiresTrustApi           = apiDefinition("requires-trust-api").doesRequireTrust
    val apiWithOnlyRetiredVersions = apiDefinition("api-with-retired-versions", apiVersion(versionOne, RETIRED), apiVersion(versionTwo, RETIRED))

    val apiWithRetiredVersions = apiDefinition("api-with-retired-versions", apiVersion(versionOne, RETIRED), apiVersion(versionTwo, STABLE))

    val apiWithPublicAndPrivateVersions =
      apiDefinition("api-with-public-and-private-versions", apiVersion(versionOne, access = PrivateApiAccess()), apiVersion(versionTwo, access = apiAccess()))

    val apiWithAllowlisting = apiDefinition("api-with-allowlisting", apiVersion(versionOne, access = PrivateApiAccess().withAllowlistedAppIds(applicationId)))

    val underTest = new ExtendedApiDefinitionForCollaboratorFetcher(
      PrincipalApiDefinitionServiceMock.aMock,
      SubordinateApiDefinitionServiceMock.aMock,
      ApplicationIdsForCollaboratorFetcherMock.aMock,
      SubscriptionsForCollaboratorFetcherMock.aMock,
      doNothingCache
    )

    val publicApiAvailability  = ApiAvailability(false, PublicApiAccess(), false, true)
    val privateApiAvailability = ApiAvailability(false, PrivateApiAccess(List(), false), false, false)

    val incomeTaxCategory = ApiCategory("INCOME_TAX")
    val vatTaxCategory    = ApiCategory("VAT")
  }

  "ExtendedApiDefinitionForCollaboratorFetcher" should {
    "return an extended api with categories from the definition" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchDefinition.willReturn(helloApiDefinition.withCategories(List(incomeTaxCategory, vatTaxCategory)))
      SubordinateApiDefinitionServiceMock.FetchDefinition.willReturnNone()

      val Some(result) = await(underTest.fetch(helloApiDefinition.serviceName, None))

      result.versions.head.productionAvailability mustBe Some(publicApiAvailability)
      result.versions.head.sandboxAvailability mustBe None
      result.categories must not be empty
      result.categories must contain(incomeTaxCategory)
      result.categories must contain(vatTaxCategory)
    }

    "return an extended api with only production availability when api only in principal" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchDefinition.willReturn(helloApiDefinition)
      SubordinateApiDefinitionServiceMock.FetchDefinition.willReturnNone()

      val Some(result) = await(underTest.fetch(helloApiDefinition.serviceName, None))

      result.versions.head.productionAvailability mustBe Some(publicApiAvailability)
      result.versions.head.sandboxAvailability mustBe None
    }

    "return an extended api with only sandbox availability when api only in subordinate" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchDefinition.willReturnNone()
      SubordinateApiDefinitionServiceMock.FetchDefinition.willReturn(helloApiDefinition)

      val Some(result) = await(underTest.fetch(helloApiDefinition.serviceName, None))

      result.versions.head.productionAvailability mustBe None
      result.versions.head.sandboxAvailability mustBe Some(publicApiAvailability)
    }

    "return an extended api with production and sandbox availability when api in both environments" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchDefinition.willReturn(helloApiDefinition)
      SubordinateApiDefinitionServiceMock.FetchDefinition.willReturn(helloApiDefinition)

      val Some(result) = await(underTest.fetch(helloApiDefinition.serviceName, None))

      result.versions must have size 1
      result.versions.head.sandboxAvailability mustBe Some(publicApiAvailability)
      result.versions.head.productionAvailability mustBe Some(publicApiAvailability)
    }

    "prefer subordinate API when it exists in both environments" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchDefinition.willReturn(helloApiDefinition.withName("hello-principal"))
      SubordinateApiDefinitionServiceMock.FetchDefinition.willReturn(helloApiDefinition.withName("hello-subordinate"))

      val Some(result) = await(underTest.fetch(helloApiDefinition.serviceName, None))

      result.name mustBe "hello-subordinate"
    }

    "prefer subordinate version when it exists in both environments" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchDefinition.willReturn(helloApiDefinition.withVersions(apiVersion(versionOne, BETA)))
      SubordinateApiDefinitionServiceMock.FetchDefinition.willReturn(helloApiDefinition.withVersions(apiVersion(versionOne, STABLE)))

      val Some(result) = await(underTest.fetch(helloApiDefinition.serviceName, None))

      result.versions must have size 1
      result.versions.head.status mustBe STABLE
    }

    "return none when api doesn't exist in any environments" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchDefinition.willReturnNone()
      SubordinateApiDefinitionServiceMock.FetchDefinition.willReturnNone()

      val result = await(underTest.fetch(helloApiDefinition.serviceName, None))

      result mustBe None
    }

    "return none when apis requires trust" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchDefinition.willReturn(requiresTrustApi)
      SubordinateApiDefinitionServiceMock.FetchDefinition.willReturn(requiresTrustApi)

      val result = await(underTest.fetch(helloApiDefinition.serviceName, None))

      result mustBe None
    }

    "filter out retired versions" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchDefinition.willReturnNone()
      SubordinateApiDefinitionServiceMock.FetchDefinition.willReturn(apiWithRetiredVersions)

      val Some(result) = await(underTest.fetch(helloApiDefinition.serviceName, None))

      result.versions must have size 1
      result.versions.head.status mustBe STABLE
    }

    "return none if all verions are retired" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchDefinition.willReturnNone()
      SubordinateApiDefinitionServiceMock.FetchDefinition.willReturn(apiWithOnlyRetiredVersions)

      val result = await(underTest.fetch(helloApiDefinition.serviceName, None))

      result mustBe None
    }

    "return public and private availability for api public and private versions " in new Setup {
      PrincipalApiDefinitionServiceMock.FetchDefinition.willReturnNone()
      SubordinateApiDefinitionServiceMock.FetchDefinition.willReturn(apiWithPublicAndPrivateVersions)

      val Some(result) = await(underTest.fetch(helloApiDefinition.serviceName, None))

      result.versions.map(_.sandboxAvailability) must contain only (Some(privateApiAvailability), Some(publicApiAvailability))
      result.versions.map(_.productionAvailability) must contain only None
    }

    "return true when application ids are matching" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchDefinition.willReturnNone()
      SubordinateApiDefinitionServiceMock.FetchDefinition.willReturn(apiWithAllowlisting)
      ApplicationIdsForCollaboratorFetcherMock.FetchAllApplicationIds.willReturnApplicationIds(applicationId)
      SubscriptionsForCollaboratorFetcherMock.willReturnSubscriptions()

      val Some(result) = await(underTest.fetch(helloApiDefinition.serviceName, email))

      result.versions.head.sandboxAvailability.map(_.authorised) mustBe Some(true)
    }

    "return false when applications ids are not matching" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchDefinition.willReturnNone()
      SubordinateApiDefinitionServiceMock.FetchDefinition.willReturn(apiWithAllowlisting)
      ApplicationIdsForCollaboratorFetcherMock.FetchAllApplicationIds.willReturnApplicationIds(ApplicationId.random)
      SubscriptionsForCollaboratorFetcherMock.willReturnSubscriptions()

      val Some(result) = await(underTest.fetch(helloApiDefinition.serviceName, email))

      result.versions.head.sandboxAvailability.map(_.authorised) mustBe Some(false)
    }

    "return true when applications ids are not matching but it is subscribed to" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchDefinition.willReturnNone()
      SubordinateApiDefinitionServiceMock.FetchDefinition.willReturn(apiWithAllowlisting)
      ApplicationIdsForCollaboratorFetcherMock.FetchAllApplicationIds.willReturnApplicationIds(ApplicationId.random)
      val apiId = ApiIdentifier(apiWithAllowlisting.context, apiWithAllowlisting.versions.head.version)
      SubscriptionsForCollaboratorFetcherMock.willReturnSubscriptions(apiId)

      val Some(result) = await(underTest.fetch(helloApiDefinition.serviceName, email))

      result.versions.head.sandboxAvailability.map(_.authorised) mustBe Some(true)
    }
  }
}
