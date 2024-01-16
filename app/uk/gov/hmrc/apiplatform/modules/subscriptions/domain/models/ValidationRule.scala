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

package uk.gov.hmrc.apiplatform.modules.subscriptions.domain.models

import cats.data.{NonEmptyList => NEL}

import play.api.libs.json.Format

import uk.gov.hmrc.apiplatform.modules.common.domain.services.SealedTraitJsonFormatting

sealed trait ValidationRule

case class RegexValidationRule(regex: String) extends ValidationRule
case object UrlValidationRule                 extends ValidationRule
case class ValidationGroup(errorMessage: String, rules: NEL[ValidationRule])

sealed trait FieldDefinitionType {
  lazy val label = FieldDefinitionType.label(this)
}

object FieldDefinitionType {

  @deprecated("We don't use URL type for any validation", since = "0.5x")
  case object URL          extends FieldDefinitionType
  case object SECURE_TOKEN extends FieldDefinitionType
  case object STRING       extends FieldDefinitionType
  case object PPNS_FIELD   extends FieldDefinitionType

  val values = Set(URL, SECURE_TOKEN, STRING, PPNS_FIELD)

  def apply(text: String): Option[FieldDefinitionType] = FieldDefinitionType.values.find(_.label == text)

  def unsafeApply(text: String): FieldDefinitionType = apply(text).getOrElse(throw new RuntimeException(s"$text is not a valid Field Definition Type"))

  def label(fdt: FieldDefinitionType): String = fdt match {
    case URL          => "URL"
    case SECURE_TOKEN => "SecureToken"
    case STRING       => "STRING"
    case PPNS_FIELD   => "PPNSField"
  }

  implicit val format: Format[FieldDefinitionType] = SealedTraitJsonFormatting.createFormatFor[FieldDefinitionType]("Field Definition Type", apply, label)
}
