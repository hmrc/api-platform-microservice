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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors

import java.{util => ju}

private[connectors] object SubscriptionFieldsConnectorDomain {

  import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.subscriptions.SubscriptionFieldsDomain._
  import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.subscriptions.AccessRequirements
  import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.SubscriptionFieldsService.DefinitionsByApiVersion
  import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.ApiIdentifier

  def toDomain(f: FieldDefinition): SubscriptionFieldDefinition = {
    SubscriptionFieldDefinition(
      name = f.name,
      description = f.description,
      shortDescription = f.shortDescription,
      `type` = f.`type`,
      hint = f.hint,
      access = f.access
    )
  }

  def toDomain(fs: AllApiFieldDefinitions): DefinitionsByApiVersion = {
    fs.apis
      .map(fd => ApiIdentifier(fd.apiContext, fd.apiVersion) -> fd.fieldDefinitions.map(toDomain))
      .toMap
  }

  case class ApplicationApiFieldValues(
      clientId: String,
      apiContext: String,
      apiVersion: String,
      fieldsId: ju.UUID,
      fields: Map[String, String])

  case class FieldDefinition(
      name: String,
      description: String,
      shortDescription: String,
      hint: String,
      `type`: String,
      access: AccessRequirements)

  case class ApiFieldDefinitions(
      apiContext: String,
      apiVersion: String,
      fieldDefinitions: List[FieldDefinition])

  case class AllApiFieldDefinitions(apis: Seq[ApiFieldDefinitions])

}
