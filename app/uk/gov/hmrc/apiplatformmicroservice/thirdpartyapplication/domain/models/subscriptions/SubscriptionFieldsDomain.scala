/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.subscriptions

import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiStatus
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.ThreeDMap
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.{ApiContext, ApiVersion}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.fields.AccessRequirements

object SubscriptionFieldsDomain {

  case class VersionSubscription(version: ApiVersionDefinition, subscribed: Boolean)

  case class ApiVersionDefinition(version: String, status: ApiStatus)

  case class SubscriptionFieldDefinition(
      name: FieldName,
      description: String,
      shortDescription: String,
      hint: String,
      `type`: String,
      access: AccessRequirements
  )

  case class FieldName(value: String) extends AnyVal

  case class FieldValue(value: String) extends AnyVal

  case class SubscriptionFieldValue(definition: SubscriptionFieldDefinition, value: FieldValue)

  type Fields = Map[FieldName, FieldValue]

  object Fields {
    val empty = Map.empty[FieldName, FieldValue]
  }
    
  type ApiFieldMap[V] = ThreeDMap.Type[ApiContext, ApiVersion, FieldName,V]


import cats.data.{NonEmptyList => NEL}

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


}
