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

import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiDefinitionTestDataHelper
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks.ThirdPartyApplicationConnectorModule
import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import org.mockito.MockitoSugar
import org.mockito.ArgumentMatchersSugar
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.UserId

class ApplicationIdsForCollaboratorFetcherSpec extends AsyncHmrcSpec with ApiDefinitionTestDataHelper {

  trait Setup extends ThirdPartyApplicationConnectorModule with MockitoSugar with ArgumentMatchersSugar {
    implicit val headerCarrier    = HeaderCarrier()
    val email                     = UserId.random
    val subordinateApplicationIds = (1 to 3).map(_ => ApplicationId.random)
    val principalApplicationIds   = (1 to 2).map(_ => ApplicationId.random)
    val underTest                 = new ApplicationIdsForCollaboratorFetcher(SubordinateThirdPartyApplicationConnectorMock.aMock, PrincipalThirdPartyApplicationConnectorMock.aMock)
  }

  "ApplicationIdsForCollaboratorFetcher" should {
    "concatenate both subordinate and principal application Ids" in new Setup {
      SubordinateThirdPartyApplicationConnectorMock.FetchApplicationsByUserId.willReturnApplicationIds(subordinateApplicationIds: _*)
      PrincipalThirdPartyApplicationConnectorMock.FetchApplicationsByUserId.willReturnApplicationIds(principalApplicationIds: _*)

      val result = await(underTest.fetch(email))

      result should contain only (subordinateApplicationIds ++ principalApplicationIds: _*)
    }

    "return subordinate application Ids if there are no matching principal applications" in new Setup {
      SubordinateThirdPartyApplicationConnectorMock.FetchApplicationsByUserId.willReturnApplicationIds(subordinateApplicationIds: _*)
      PrincipalThirdPartyApplicationConnectorMock.FetchApplicationsByUserId.willReturnApplicationIds(Seq.empty: _*)

      val result = await(underTest.fetch(email))

      result should contain only (subordinateApplicationIds: _*)
    }

    "return principal application Ids if there are no matching subordinate applications" in new Setup {
      SubordinateThirdPartyApplicationConnectorMock.FetchApplicationsByUserId.willReturnApplicationIds(Seq.empty: _*)
      PrincipalThirdPartyApplicationConnectorMock.FetchApplicationsByUserId.willReturnApplicationIds(principalApplicationIds: _*)

      val result = await(underTest.fetch(email))

      result should contain only (principalApplicationIds: _*)
    }

    "return an empty sequence if there are no matching applications in any environment" in new Setup {
      SubordinateThirdPartyApplicationConnectorMock.FetchApplicationsByUserId.willReturnApplicationIds(Seq.empty: _*)
      PrincipalThirdPartyApplicationConnectorMock.FetchApplicationsByUserId.willReturnApplicationIds(Seq.empty: _*)

      val result = await(underTest.fetch(email))

      result shouldBe empty
    }

    "return principal application Ids if something goes wrong in subordinate" in new Setup {
      val expectedExceptionMessage = "something went wrong"
      SubordinateThirdPartyApplicationConnectorMock.FetchApplicationsByUserId.willThrowException(new RuntimeException(expectedExceptionMessage))
      PrincipalThirdPartyApplicationConnectorMock.FetchApplicationsByUserId.willReturnApplicationIds(principalApplicationIds: _*)

      val result = await(underTest.fetch(email))

      result should contain only (principalApplicationIds: _*)
    }

    "throw exception if something goes wrong in principal" in new Setup {
      val expectedExceptionMessage = "something went wrong"
      SubordinateThirdPartyApplicationConnectorMock.FetchApplicationsByUserId.willReturnApplicationIds(Seq.empty: _*)
      PrincipalThirdPartyApplicationConnectorMock.FetchApplicationsByUserId.willThrowException(new RuntimeException(expectedExceptionMessage))

      val ex = intercept[RuntimeException] {
        await(underTest.fetch(email))
      }

      ex.getMessage shouldBe expectedExceptionMessage
    }
  }
}
