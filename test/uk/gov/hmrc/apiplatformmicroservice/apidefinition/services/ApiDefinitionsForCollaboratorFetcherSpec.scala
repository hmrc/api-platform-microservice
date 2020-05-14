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

import uk.gov.hmrc.apiplatformmicroservice.util.mocks.{ApiDefinitionConnectorModule, ApplicationIdsForCollaboratorFetcherModule}
import uk.gov.hmrc.apiplatformmicroservice.util.{ApiDefinitionTestDataHelper, AsyncHmrcSpec}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class ApiDefinitionsForCollaboratorFetcherSpec extends AsyncHmrcSpec with ApiDefinitionTestDataHelper {

  trait Setup extends ApiDefinitionConnectorModule with ApplicationIdsForCollaboratorFetcherModule {
    implicit val headerCarrier = HeaderCarrier()
    val fakeEmail = "joebloggs@example.com"
    val fakeApiName = "hello-api"
    val fakeApiDefinition = apiDefinition(fakeApiName)
    val underTest = new ApiDefinitionsForCollaboratorFetcher(ApiDefinitionConnectorMock.aMock, ApplicationIdsForCollaboratorFetcherMock.aMock)
  }

  "ApiDefinitionsForCollaboratorFetcher" should {
    "return the public APIs" in new Setup {
      ApiDefinitionConnectorMock.FetchAllApiDefinitions.willReturnApiDefinitions(fakeApiDefinition)
      ApplicationIdsForCollaboratorFetcherMock.FetchAllApplicationIds.willReturnApplicationIds(Seq.empty: _*)

      val result = await(underTest(fakeEmail))

      result mustBe Seq(fakeApiDefinition)
    }

  }
}
