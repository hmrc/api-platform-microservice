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

package uk.gov.hmrc.apiplatformmicroservice.apidefinition.services

import uk.gov.hmrc.apiplatformmicroservice.apidefinition.mocks.ApiDefinitionsForCollaboratorFetcherModule
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.APIStatus.STABLE
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiDefinitionTestDataHelper
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks.SubscriptionsForCollaboratorFetcherModule
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.models.APIIdentifier
import uk.gov.hmrc.apiplatformmicroservice.util.AsyncHmrcSpec
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class SubscribedApiDefinitionsForCollaboratorFetcherSpec extends AsyncHmrcSpec with ApiDefinitionTestDataHelper {

  trait Setup extends ApiDefinitionsForCollaboratorFetcherModule with SubscriptionsForCollaboratorFetcherModule {
    implicit val headerCarrier = HeaderCarrier()
    val email = "joebloggs@example.com"
    val helloWorldDefinition = apiDefinition("hello-world", Seq(apiVersion("1.0", STABLE), apiVersion("2.0", STABLE)))
    val helloAgentsDefinition = apiDefinition("hello-agents", Seq(apiVersion("1.0", STABLE), apiVersion("2.0", STABLE)))
    val helloVatDefinition = apiDefinition("hello-vat", Seq(apiVersion("1.0", STABLE)))

    val underTest = new SubscribedApiDefinitionsForCollaboratorFetcher(ApiDefinitionsForCollaboratorFetcherMock.aMock,
      SubscriptionsForCollaboratorFetcherMock.aMock)
  }

  "SubscribedApiDefinitionsForCollaboratorFetcher" should {
    "return only the APIs that the collaborator is subscribed to" in new Setup {
      ApiDefinitionsForCollaboratorFetcherMock.willReturnApiDefinitions(helloWorldDefinition, helloAgentsDefinition, helloVatDefinition)
      SubscriptionsForCollaboratorFetcherMock
        .willReturnSubscriptions(APIIdentifier("hello-world", "1.0"), APIIdentifier("hello-world", "2.0"), APIIdentifier("hello-vat", "1.0"))

      val result = await(underTest.fetch(email))

      result shouldBe Seq(helloWorldDefinition, helloVatDefinition)
    }

    "filter out the versions that the collaborator is not subscribed to" in new Setup {
      ApiDefinitionsForCollaboratorFetcherMock.willReturnApiDefinitions(helloWorldDefinition, helloAgentsDefinition, helloVatDefinition)
      SubscriptionsForCollaboratorFetcherMock.willReturnSubscriptions(APIIdentifier("hello-world", "2.0"))

      val result = await(underTest.fetch(email))

      result.head.versions.map(_.version) should contain only "2.0"
    }
  }
}
