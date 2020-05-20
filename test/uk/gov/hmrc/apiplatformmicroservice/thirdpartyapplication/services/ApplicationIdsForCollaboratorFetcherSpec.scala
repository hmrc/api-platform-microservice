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

import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiDefinitionTestDataHelper
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks.ThirdPartyApplicationConnectorModule
import uk.gov.hmrc.apiplatformmicroservice.util.AsyncHmrcSpec
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class ApplicationIdsForCollaboratorFetcherSpec extends AsyncHmrcSpec with ApiDefinitionTestDataHelper {

  trait Setup extends ThirdPartyApplicationConnectorModule {
    implicit val headerCarrier = HeaderCarrier()
    val email = "joebloggs@example.com"
    val sandboxApplicationIds = Seq("s1", "s2", "s3")
    val productionApplicationIds = Seq("p1", "p2")
    val underTest = new ApplicationIdsForCollaboratorFetcher(SandboxThirdPartyApplicationConnectorMock.aMock,
      ProductionThirdPartyApplicationConnectorMock.aMock)
  }

  "ApplicationIdsForCollaboratorFetcher" should {
    "concatenate both sandbox and production application Ids" in new Setup {
      SandboxThirdPartyApplicationConnectorMock.FetchApplicationsByEmail.willReturnApplicationIds(sandboxApplicationIds: _*)
      ProductionThirdPartyApplicationConnectorMock.FetchApplicationsByEmail.willReturnApplicationIds(productionApplicationIds: _*)

      val result = await(underTest(email))

      result mustBe Seq("s1", "s2", "s3", "p1", "p2")
    }

    "return sandbox application Ids if there are no matching production applications" in new Setup {
      SandboxThirdPartyApplicationConnectorMock.FetchApplicationsByEmail.willReturnApplicationIds(sandboxApplicationIds: _*)
      ProductionThirdPartyApplicationConnectorMock.FetchApplicationsByEmail.willReturnApplicationIds(Seq.empty: _*)

      val result = await(underTest(email))

      result mustBe sandboxApplicationIds
    }

    "return production application Ids if there are no matching sandbox applications" in new Setup {
      SandboxThirdPartyApplicationConnectorMock.FetchApplicationsByEmail.willReturnApplicationIds(Seq.empty: _*)
      ProductionThirdPartyApplicationConnectorMock.FetchApplicationsByEmail.willReturnApplicationIds(productionApplicationIds: _*)

      val result = await(underTest(email))

      result mustBe productionApplicationIds
    }

    "return an empty sequence if there are no matching applications in any environment" in new Setup {
      SandboxThirdPartyApplicationConnectorMock.FetchApplicationsByEmail.willReturnApplicationIds(Seq.empty: _*)
      ProductionThirdPartyApplicationConnectorMock.FetchApplicationsByEmail.willReturnApplicationIds(Seq.empty: _*)

      val result = await(underTest(email))

      result mustBe Seq.empty
    }

    "return production application Ids if something goes wrong in sandbox" in new Setup {
      val expectedExceptionMessage = "something went wrong"
      SandboxThirdPartyApplicationConnectorMock.FetchApplicationsByEmail.willThrowException(new RuntimeException(expectedExceptionMessage))
      ProductionThirdPartyApplicationConnectorMock.FetchApplicationsByEmail.willReturnApplicationIds(productionApplicationIds: _*)

      val result = await(underTest(email))

      result mustBe productionApplicationIds
    }

    "throw exception if something goes wrong in production" in new Setup {
      val expectedExceptionMessage = "something went wrong"
      SandboxThirdPartyApplicationConnectorMock.FetchApplicationsByEmail.willReturnApplicationIds(Seq.empty: _*)
      ProductionThirdPartyApplicationConnectorMock.FetchApplicationsByEmail.willThrowException(new RuntimeException(expectedExceptionMessage))

      val ex = intercept[RuntimeException] {
        await(underTest(email))
      }

      ex.getMessage mustBe expectedExceptionMessage
    }
  }
}
