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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications

import enumeratum.{Enum, EnumEntry}
import enumeratum.PlayJsonEnum

sealed trait OverrideType extends EnumEntry

object OverrideType extends Enum[OverrideType] with PlayJsonEnum[OverrideType] {
  val values = findValues

  final case object PERSIST_LOGIN_AFTER_GRANT extends OverrideType
  final case object GRANT_WITHOUT_TAXPAYER_CONSENT extends OverrideType
  final case object SUPPRESS_IV_FOR_AGENTS extends OverrideType
  final case object SUPPRESS_IV_FOR_ORGANISATIONS extends OverrideType
  final case object SUPPRESS_IV_FOR_INDIVIDUALS extends OverrideType
}
