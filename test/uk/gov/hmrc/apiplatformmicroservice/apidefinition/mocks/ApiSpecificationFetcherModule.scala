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

package uk.gov.hmrc.apiplatformmicroservice.apidefinition.mocks

import scala.concurrent.Future.successful

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatestplus.play.PlaySpec

import play.api.libs.json.JsValue

import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApiVersionNbr
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.services.ApiSpecificationFetcher

trait ApiSpecificationFetcherModule extends PlaySpec with MockitoSugar with ArgumentMatchersSugar {

  object ApiSpecificationFetcherMock {
    val aMock = mock[ApiSpecificationFetcher]

    object Fetch {

      def willReturn(response: JsValue): Unit = {
        when(aMock.fetch(*, *[ApiVersionNbr])(*)).thenReturn(successful(Some(response)))
      }

      def willReturnNotFound(): Unit = {
        when(aMock.fetch(*, *[ApiVersionNbr])(*)).thenReturn(successful(None))
      }
    }
  }
}
