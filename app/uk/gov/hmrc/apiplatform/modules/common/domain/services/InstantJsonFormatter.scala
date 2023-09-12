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

package uk.gov.hmrc.apiplatform.modules.common.domain.services

import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}
import java.time.temporal.ChronoField._
import java.time.temporal.ChronoUnit
import java.time.{Instant, ZoneId}

import play.api.libs.json._

object InstantJsonFormatter {

  val lenientFormatter: DateTimeFormatter = new DateTimeFormatterBuilder()
    .parseLenient()
    .parseCaseInsensitive()
    .appendPattern("uuuu-MM-dd['T'HH:mm:ss[.SSS][Z]['Z']]")
    .parseDefaulting(NANO_OF_SECOND, 0)
    .parseDefaulting(SECOND_OF_MINUTE, 0)
    .parseDefaulting(MINUTE_OF_HOUR, 0)
    .parseDefaulting(HOUR_OF_DAY, 0)
    .toFormatter
    .withZone(ZoneId.of("UTC"))

  val lenientInstantReads: Reads[Instant] = Reads.instantReads(lenientFormatter).map(_.truncatedTo(ChronoUnit.MILLIS))

  object WithTimeZone {

    val instantWithTimeZoneWrites: Writes[Instant] = Writes.temporalWrites(
      new DateTimeFormatterBuilder()
        .appendPattern("uuuu-MM-dd'T'HH:mm:ss.SSS'Z'")
        .toFormatter
        .withZone(ZoneId.of("UTC"))
    )

    implicit val instantWithTimeZoneFormat: Format[Instant] = Format(lenientInstantReads, instantWithTimeZoneWrites)
  }

  object NoTimeZone {

    val instantNoTimeZoneWrites: Writes[Instant] = Writes.temporalWrites(
      new DateTimeFormatterBuilder()
        .appendPattern("uuuu-MM-dd'T'HH:mm:ss.SSS")
        .toFormatter
        .withZone(ZoneId.of("UTC"))
    )

    implicit val instantNoTimeZoneFormat: Format[Instant] = Format(lenientInstantReads, instantNoTimeZoneWrites)
  }
}
