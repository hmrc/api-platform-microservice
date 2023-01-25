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

package uk.gov.hmrc.apiplatformmicroservice.common

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.Json
import play.api.libs.json.EnvReads
import java.time.LocalDateTime
import java.time.Instant
import play.api.libs.json._
import play.api.libs.json.EnvWrites
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter
import java.time.ZoneOffset

class DateTimeInstantConversionSpec extends AnyWordSpec with Matchers {
  
  object MyReads extends EnvReads with EnvWrites
  
  val ldt = LocalDateTime.of(2022,12,12,12,12,10).plus(100, ChronoUnit.MILLIS)
  val instant = ldt.toInstant(ZoneOffset.UTC)

  "inbound over the wire localdatetime" should {
    val text = "2022-12-12T12:12:10.1"
  
    val dateText = s""""$text""""
    
    
    "be read as a localdatetime" in {
      import MyReads._
      Json.fromJson[LocalDateTime](Json.parse(dateText)) shouldBe JsSuccess(ldt)
    }

    "be read as an instant" in {
      val fixMe: String => String = (in) => {
        if(in.endsWith("Z"))
          in
        else 
          s"${in}Z"
      }

      implicit val safeInstantReader = MyReads.instantReads(DateTimeFormatter.ISO_INSTANT, fixMe)

      Json.fromJson[Instant](Json.parse(dateText)) shouldBe JsSuccess(instant)

    }
  }

  "inbound over the wire instant" should {
    val ldt = LocalDateTime.of(2022,12,12,12,12,10).plus(1, ChronoUnit.MILLIS)
    val instant = ldt.toInstant(ZoneOffset.UTC)

    val text = "2022-12-12T12:12:10.001Z"
    
    val dateText = s""""$text""""
    

    "be read as a localdatetime" in {
      import MyReads._
      Json.fromJson[LocalDateTime](Json.parse(dateText)) shouldBe JsSuccess(ldt)
    }

    "be read as an instant" in {
      implicit val tolerantInstantReader = MyReads.instantReads(DateTimeFormatter.ISO_INSTANT, (in) => if(in.last == 'Z') in else s"${in}Z")

      Json.fromJson[Instant](Json.parse(dateText)) shouldBe JsSuccess(instant)

    }
  }
}