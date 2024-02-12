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

import org.apache.pekko.Done

import play.api.cache.AsyncCacheApi
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.mocks.ApiDefinitionServiceModule
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models._
import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks.{ApplicationIdsForCollaboratorFetcherModule, SubscriptionsForCollaboratorFetcherModule}

class ExtendedApiDefinitionForCollaboratorFetcherSpec extends AsyncHmrcSpec with ApiDefinitionTestDataHelper {

  private val versionOne = ApiVersionNbr("1.0")
  private val versionTwo = ApiVersionNbr("2.0")

  val doNothingCache = new AsyncCacheApi {
    def set(key: String, value: Any, expiration: Duration = Duration.Inf): Future[Done] = Future.successful(Done)
    def remove(key: String): Future[Done]                                               = Future.successful(Done)

    def getOrElseUpdate[A: ClassTag](key: String, expiration: Duration = Duration.Inf)(orElse: => Future[A]): Future[A] = orElse

    def get[T: ClassTag](key: String): Future[Option[T]] = Future.successful(None)

    def removeAll(): Future[Done] = Future.successful(Done)
  }

  trait Setup extends ApiDefinitionServiceModule with ApplicationIdsForCollaboratorFetcherModule with SubscriptionsForCollaboratorFetcherModule {
    implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
    val email                                 = Some(UserId.random)
    val applicationId                         = ApplicationId.random
    val helloApiDefinition                    = apiDefinition("hello-api")
    val apiWithOnlyRetiredVersions            = apiDefinition("api-with-retired-versions", apiVersion(versionOne, ApiStatus.RETIRED), apiVersion(versionTwo, ApiStatus.RETIRED))

    val apiWithRetiredVersions = apiDefinition("api-with-retired-versions", apiVersion(versionOne, ApiStatus.RETIRED), apiVersion(versionTwo, ApiStatus.STABLE))

    val apiWithPrivateVersion =
      apiDefinition("api-with-only-private-versions", apiVersion(versionOne, access = ApiAccess.Private(false)))

    val apiWithPublicAndPrivateVersions =
      apiDefinition("api-with-public-and-private-versions", apiVersion(versionOne, access = ApiAccess.Private(false)), apiVersion(versionTwo, access = apiAccess()))

    val underTest = new ExtendedApiDefinitionForCollaboratorFetcher(
      PrincipalApiDefinitionServiceMock.aMock,
      SubordinateApiDefinitionServiceMock.aMock,
      ApplicationIdsForCollaboratorFetcherMock.aMock,
      SubscriptionsForCollaboratorFetcherMock.aMock,
      doNothingCache
    )

    val publicApiAvailability  = ApiAvailability(false, ApiAccess.PUBLIC, false, true)
    val privateApiAvailability = ApiAvailability(false, ApiAccess.Private(false), false, false)

    val incomeTaxCategory = ApiCategory.INCOME_TAX_MTD
    val vatTaxCategory    = ApiCategory.VAT
  }

  "ExtendedApiDefinitionForCollaboratorFetcher" should {
    "return an extended api with categories from the definition" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchDefinition.willReturn(helloApiDefinition.withCategories(List(incomeTaxCategory, vatTaxCategory)))
      SubordinateApiDefinitionServiceMock.FetchDefinition.willReturnNone()

      val result = await(underTest.fetch(helloApiDefinition.serviceName, None)).value

      result.versions.head.productionAvailability mustBe Some(publicApiAvailability)
      result.versions.head.sandboxAvailability mustBe None
      result.categories should not be empty
      result.categories should contain(incomeTaxCategory)
      result.categories should contain(vatTaxCategory)
    }

    "return an extended api with only production availability when api only in principal" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchDefinition.willReturn(helloApiDefinition)
      SubordinateApiDefinitionServiceMock.FetchDefinition.willReturnNone()

      val result = await(underTest.fetch(helloApiDefinition.serviceName, None)).value

      result.versions.head.productionAvailability mustBe Some(publicApiAvailability)
      result.versions.head.sandboxAvailability mustBe None
    }

    "return an extended api with only sandbox availability when api only in subordinate" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchDefinition.willReturnNone()
      SubordinateApiDefinitionServiceMock.FetchDefinition.willReturn(helloApiDefinition)

      val result = await(underTest.fetch(helloApiDefinition.serviceName, None)).value

      result.versions.head.productionAvailability mustBe None
      result.versions.head.sandboxAvailability mustBe Some(publicApiAvailability)
    }

    "return an extended api with production and sandbox availability when api in both environments" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchDefinition.willReturn(helloApiDefinition)
      SubordinateApiDefinitionServiceMock.FetchDefinition.willReturn(helloApiDefinition)

      val result = await(underTest.fetch(helloApiDefinition.serviceName, None)).value

      result.versions should have size 1
      result.versions.head.sandboxAvailability mustBe Some(publicApiAvailability)
      result.versions.head.productionAvailability mustBe Some(publicApiAvailability)
    }

    "prefer subordinate API when it exists in both environments" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchDefinition.willReturn(helloApiDefinition.withName("hello-principal"))
      SubordinateApiDefinitionServiceMock.FetchDefinition.willReturn(helloApiDefinition.withName("hello-subordinate"))

      val result = await(underTest.fetch(helloApiDefinition.serviceName, None)).value

      result.name mustBe "hello-subordinate"
    }

    "prefer subordinate version when it exists in both environments" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchDefinition.willReturn(helloApiDefinition.withVersions(apiVersion(versionOne, ApiStatus.BETA)))
      SubordinateApiDefinitionServiceMock.FetchDefinition.willReturn(helloApiDefinition.withVersions(apiVersion(versionOne, ApiStatus.STABLE)))

      val result = await(underTest.fetch(helloApiDefinition.serviceName, None)).value

      result.versions should have size 1
      result.versions.head.status mustBe ApiStatus.STABLE
    }

    "return none when api doesn't exist in any environments" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchDefinition.willReturnNone()
      SubordinateApiDefinitionServiceMock.FetchDefinition.willReturnNone()

      val result = await(underTest.fetch(helloApiDefinition.serviceName, None))

      result mustBe None
    }

    "filter out retired versions" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchDefinition.willReturnNone()
      SubordinateApiDefinitionServiceMock.FetchDefinition.willReturn(apiWithRetiredVersions)

      val result = await(underTest.fetch(helloApiDefinition.serviceName, None)).value

      result.versions should have size 1
      result.versions.head.status mustBe ApiStatus.STABLE
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

      val result = await(underTest.fetch(helloApiDefinition.serviceName, None))

      result.value.versions.map(_.sandboxAvailability) should contain.only(Some(privateApiAvailability), Some(publicApiAvailability))
      result.value.versions.map(_.productionAvailability) should contain only None
    }

    "return true when applications ids are subscribed to the api" in new Setup {
      PrincipalApiDefinitionServiceMock.FetchDefinition.willReturnNone()
      SubordinateApiDefinitionServiceMock.FetchDefinition.willReturn(apiWithPrivateVersion)
      ApplicationIdsForCollaboratorFetcherMock.FetchAllApplicationIds.willReturnApplicationIds(ApplicationId.random)
      val apiId = ApiIdentifier(apiWithPrivateVersion.context, apiWithPrivateVersion.versions.keySet.head)
      SubscriptionsForCollaboratorFetcherMock.willReturnSubscriptions(apiId)

      val result = await(underTest.fetch(helloApiDefinition.serviceName, email)).value

      result.versions.head.sandboxAvailability.map(_.authorised) mustBe Some(true)
    }
  }
}
