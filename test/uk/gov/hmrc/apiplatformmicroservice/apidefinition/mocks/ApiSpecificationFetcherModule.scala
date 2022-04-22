/*
 * Copyright 2022 HM Revenue & Customs
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

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.services.ApiSpecificationFetcher
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.JsValue 
import scala.concurrent.Future.successful
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiVersion

trait ApiSpecificationFetcherModule extends PlaySpec with MockitoSugar with ArgumentMatchersSugar {
  object ApiSpecificationFetcherMock {
    val aMock = mock[ApiSpecificationFetcher]

    object Fetch {
      def willReturn(response: JsValue) {
        when(aMock.fetch(*, *[ApiVersion])(*)).thenReturn(successful(Some(response)))
      }

      def willReturnNotFound() {
        when(aMock.fetch(*, *[ApiVersion])(*)).thenReturn(successful(None))
      }
    }
  }
}