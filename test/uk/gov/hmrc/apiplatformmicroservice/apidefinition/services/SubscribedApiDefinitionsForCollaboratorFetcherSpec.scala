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

package uk.gov.hmrc.apiplatformmicroservice.apidefinition.services

import uk.gov.hmrc.apiplatformmicroservice.apidefinition.mocks.ApiDefinitionsForCollaboratorFetcherModule
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.APIStatus.STABLE
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.{ApiContext, ApiDefinitionTestDataHelper, ApiIdentifier, ApiVersion}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks.SubscriptionsForCollaboratorFetcherModule
import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.EmailIdentifier

class SubscribedApiDefinitionsForCollaboratorFetcherSpec extends AsyncHmrcSpec with ApiDefinitionTestDataHelper {

  private val helloWorldContext = ApiContext("hello-world")
  private val versionOne = ApiVersion("1.0")
  private val versionTwo = ApiVersion("2.0")

  trait Setup extends ApiDefinitionsForCollaboratorFetcherModule with SubscriptionsForCollaboratorFetcherModule {
    implicit val headerCarrier = HeaderCarrier()
    val email = EmailIdentifier("joebloggs@example.com")
    val helloWorldDefinition = apiDefinition(helloWorldContext.value, apiVersion(versionOne, STABLE), apiVersion(versionTwo, STABLE))
    val helloAgentsDefinition = apiDefinition("hello-agents", apiVersion(versionOne, STABLE), apiVersion(versionTwo, STABLE))
    val helloVatDefinition = apiDefinition("hello-vat", apiVersion(versionOne, STABLE))

    val underTest = new SubscribedApiDefinitionsForCollaboratorFetcher(ApiDefinitionsForCollaboratorFetcherMock.aMock, SubscriptionsForCollaboratorFetcherMock.aMock)
  }

  "SubscribedApiDefinitionsForCollaboratorFetcher" should {
    "return only the APIs that the collaborator is subscribed to" in new Setup {
      ApiDefinitionsForCollaboratorFetcherMock.willReturnApiDefinitions(helloWorldDefinition, helloAgentsDefinition, helloVatDefinition)
      SubscriptionsForCollaboratorFetcherMock
        .willReturnSubscriptions(
          ApiIdentifier(helloWorldContext, versionOne),
          models.ApiIdentifier(helloWorldContext, versionTwo),
          models.ApiIdentifier(ApiContext("hello-vat"), versionOne)
        )

      val result = await(underTest.fetch(email))

      result shouldBe List(helloWorldDefinition, helloVatDefinition)
    }

    "filter out the versions that the collaborator is not subscribed to" in new Setup {
      ApiDefinitionsForCollaboratorFetcherMock.willReturnApiDefinitions(helloWorldDefinition, helloAgentsDefinition, helloVatDefinition)
      SubscriptionsForCollaboratorFetcherMock.willReturnSubscriptions(models.ApiIdentifier(helloWorldContext, versionTwo))

      val result = await(underTest.fetch(email))

      result.head.versions.map(_.version) should contain only versionTwo
    }
  }
}
