/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.fields

import cats.data.{NonEmptyList => NEL}
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.FieldName

sealed trait ValidationRule

case class RegexValidationRule(regex: String) extends ValidationRule
case object UrlValidationRule extends ValidationRule
case class ValidationGroup(errorMessage: String, rules: NEL[ValidationRule])

object FieldDefinitionType extends Enumeration {
  type FieldDefinitionType = Value

  @deprecated("We don't use URL type for any validation", since = "0.5x")
  val URL = Value("URL")
  val SECURE_TOKEN = Value("SecureToken")
  val STRING = Value("STRING")
  val PPNS_FIELD = Value("PPNSField")
}

case class FieldDefinition(
    name: FieldName,
    description: String,
    hint: String = "",
    `type`: FieldDefinitionType.FieldDefinitionType,
    shortDescription: String,
    validation: Option[ValidationGroup],
    access: AccessRequirements = AccessRequirements.Default)
