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

package uk.gov.hmrc.apiplatform.modules.apis.domain.models

import uk.gov.hmrc.apiplatform.modules.common.utils.SealedTraitJsonFormatting

sealed trait AuthType

object AuthType {
  case object NONE        extends AuthType
  case object APPLICATION extends AuthType
  case object USER        extends AuthType

  val values = Set[AuthType](NONE, APPLICATION, USER)

  def apply(text: String): Option[AuthType] = {
    AuthType.values.find(_.toString == text.toUpperCase())
  }

  def unsafeApply(text: String): AuthType =
    apply(text).getOrElse(throw new RuntimeException(s"$text is not a valid Auth Type"))

  implicit val formatAuthType = SealedTraitJsonFormatting.createFormatFor[AuthType]("AuthType", apply)

}
