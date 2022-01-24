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
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.{FieldName, ThreeDMap}
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.{ApiContext, ApiVersion}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.fields.AccessRequirements

object SubscriptionFieldsDomain {

  case class VersionSubscription(version: ApiVersionDefinition, subscribed: Boolean)

  case class ApiVersionDefinition(version: String, status: ApiStatus)

  case class SubscriptionFieldDefinition(
      name: String,
      description: String,
      shortDescription: String,
      hint: String,
      `type`: String,
      access: AccessRequirements)

  type Fields = Map[String, String]

  object Fields {
    val empty = Map.empty[String, String]
  }
    
  type ApiFieldMap[V] = ThreeDMap.Type[ApiContext, ApiVersion, FieldName,V]
}
