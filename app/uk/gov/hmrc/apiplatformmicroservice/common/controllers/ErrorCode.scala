/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.apiplatformmicroservice.common.controllers

import play.api.libs.json.Format

import uk.gov.hmrc.apiplatform.modules.common.domain.services.SealedTraitJsonFormatting

sealed trait ErrorCode

object ErrorCode {

  case object UNKNOWN_ERROR               extends ErrorCode
  case object SUBSCRIPTION_ALREADY_EXISTS extends ErrorCode
  case object APPLICATION_NOT_FOUND       extends ErrorCode
  case object SUBSCRIPTION_DENIED         extends ErrorCode

  val values = Set(UNKNOWN_ERROR, SUBSCRIPTION_ALREADY_EXISTS, APPLICATION_NOT_FOUND, SUBSCRIPTION_DENIED)

  def apply(text: String): Option[ErrorCode] = ErrorCode.values.find(_.toString() == text.toUpperCase)

  def unsafeApply(text: String): ErrorCode = apply(text).getOrElse(throw new RuntimeException(s"$text is not a valid Error Code"))

  implicit val format: Format[ErrorCode] = SealedTraitJsonFormatting.createFormatFor[ErrorCode]("Error Code", apply)
}
