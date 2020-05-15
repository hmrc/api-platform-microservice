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

package uk.gov.hmrc.apiplatformmicroservice.apidefinition.models

import play.api.libs.json.{JsResultException, Json}
import uk.gov.hmrc.play.test.UnitSpec

class EnumJsonSpec extends UnitSpec {

  object Color extends Enumeration{
    type Color = Value
    val RED, BLUE, GREEN = Value
  }

  implicit val formatColor = EnumJson.enumFormat(Color)

  "EnumJson" should {
    "parse JSON to an Enumeration" in  {
      val result = Json.parse("""{"color":"BLUE"}""").\("color").as[Color.Color]
      result shouldBe Color.BLUE
    }

    "format Enumeration as JSON" in {
      val result = Json.toJson(Color.RED)
      result.toString() shouldBe """"RED""""
    }

    "throw exception when color does not exist in the enumeration" in {
      val ex = intercept[JsResultException] {
        Json.parse("""{"color":"ORANGE"}""").\("color").as[Color.Color]
      }
      ex.getMessage should include ("it does not contain 'ORANGE'")
    }

    "throw exception when the value is not a string" in {
      val ex = intercept[JsResultException] {
        Json.parse("""{"color":123}""").\("color").as[Color.Color]
      }
      ex.getMessage should include ("String value expected")
    }
  }
}
