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

import play.api.libs.json.{JsSuccess, Json}

import uk.gov.hmrc.apiplatform.modules.common.utils.HmrcSpec
import uk.gov.hmrc.apiplatform.modules.subscriptions.domain.models.DevhubAccessRequirement._
import uk.gov.hmrc.apiplatform.modules.subscriptions.domain.models._
import uk.gov.hmrc.apiplatform.modules.subscriptions.domain.services.FieldsJsonFormatters

class AccessRequirementsJsonFormatterSpec extends HmrcSpec with FieldsJsonFormatters {
  "JsonFormatter" should {
    "Read devhub" in {
      Json.fromJson[DevhubAccessRequirements](Json.parse("""{ "read": "adminOnly", "write" : "noOne" }""")) shouldBe JsSuccess(DevhubAccessRequirements(AdminOnly, NoOne))

    }
    "Read" in {
      Json.fromJson[AccessRequirements](Json.parse("""{ "devhub" : { "read": "anyone", "write" : "adminOnly" }}""")) shouldBe JsSuccess(AccessRequirements(DevhubAccessRequirements(
        Anyone,
        AdminOnly
      )))
    }
  }

}
