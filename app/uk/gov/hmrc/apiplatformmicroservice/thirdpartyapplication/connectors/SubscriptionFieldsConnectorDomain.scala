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

import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.{ApiDefinitionJsonFormatters, ApiVersion}
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.FieldName
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.services.ApplicationJsonFormatters
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.fields.FieldDefinition
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.services.FieldsJsonFormatters

private[connectors] object SubscriptionFieldsConnectorDomain {
  import cats.data.{NonEmptyList => NEL}

  import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiContext
  import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.{ClientId, FieldValue}
  import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.fields._

  case class ApiFieldDefinitions(apiContext: ApiContext, apiVersion: ApiVersion, fieldDefinitions: NEL[FieldDefinition])

  case class BulkApiFieldDefinitionsResponse(apis: Seq[ApiFieldDefinitions])

  case class ApplicationApiFieldValues(
      clientId: ClientId,
      apiContext: ApiContext,
      apiVersion: ApiVersion,
      fieldsId: ju.UUID,
      fields: Map[FieldName, FieldValue])

  case class SubscriptionFields(apiContext: ApiContext, apiVersion: ApiVersion, fields: Map[FieldName, FieldValue])

  case class BulkSubscriptionFieldsResponse(subscriptions: Seq[SubscriptionFields])

  def asMapOfMapsOfFieldDefns(fieldDefs: Seq[ApiFieldDefinitions]): Map[ApiContext, Map[ApiVersion, Map[FieldName, FieldDefinition]]] = {
    import cats._
    import cats.implicits._
    type MapType = Map[ApiVersion, Map[FieldName, FieldDefinition]]

    // Shortcut combining as we know there will never be records for the same version for the same context
    implicit def monoidVersions: Monoid[MapType] =
      new Monoid[MapType] {

        override def combine(x: MapType, y: MapType): MapType = x ++ y

        override def empty: MapType = Map.empty
      }

    Monoid.combineAll(
      fieldDefs.map(s => Map(s.apiContext -> Map(s.apiVersion -> s.fieldDefinitions.map(fd => fd.name -> fd).toList.toMap)))
    )
  }

  def asMapOfMaps(subscriptions: Seq[SubscriptionFields]): Map[ApiContext, Map[ApiVersion, Map[FieldName, FieldValue]]] = {
    import cats._
    import cats.implicits._
    type MapType = Map[ApiVersion, Map[FieldName, FieldValue]]

    // Shortcut combining as we know there will never be records for the same version for the same context
    implicit def monoidVersions: Monoid[MapType] =
      new Monoid[MapType] {

        override def combine(x: MapType, y: MapType): MapType = x ++ y

        override def empty: MapType = Map.empty
      }

    Monoid.combineAll(
      subscriptions.map(s => Map(s.apiContext -> Map(s.apiVersion -> s.fields)))
    )
  }

  trait SubscriptionJsonFormatters extends ApiDefinitionJsonFormatters with ApplicationJsonFormatters with FieldsJsonFormatters {
    import play.api.libs.json.Json

    implicit val readsApplicationApiFieldValues = Json.reads[ApplicationApiFieldValues]

    implicit val readsSubscriptionFields = Json.reads[SubscriptionFields]
    implicit val readsBulkSubscriptionFieldsResponse = Json.reads[BulkSubscriptionFieldsResponse]

    implicit val readsApiFieldDefinitions = Json.reads[ApiFieldDefinitions]
    implicit val readsBulkApiFieldDefinitionsResponse = Json.reads[BulkApiFieldDefinitionsResponse]

  }

  object SubscriptionJsonFormatters extends SubscriptionJsonFormatters
}
