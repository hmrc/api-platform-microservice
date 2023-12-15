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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.services

import uk.gov.hmrc.apiplatform.modules.common.utils.HmrcSpec
import uk.gov.hmrc.apiplatform.modules.subscriptions.domain.models._

class FieldNameAndValueSpec extends HmrcSpec with ApplicationJsonFormatters {
  "JsonFormatter" should {
    "Read raw map" in {
      import play.api.libs.json._
      Json.fromJson[Map[FieldName, FieldValue]](Json.parse("""{ "a": "1", "b": "2" }""")) shouldBe JsSuccess(Map(FieldName("a") -> FieldValue("1"), FieldName("b") -> FieldValue("2")))
    }
  }
}
