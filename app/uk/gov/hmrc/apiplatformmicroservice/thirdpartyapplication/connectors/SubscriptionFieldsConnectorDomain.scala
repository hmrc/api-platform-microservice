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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors

import java.util.UUID

import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.applications.subscriptions.domain.models._
import uk.gov.hmrc.apiplatform.modules.subscriptions.domain.models._
import uk.gov.hmrc.apiplatform.modules.subscriptions.domain.services.FieldsJsonFormatters
import uk.gov.hmrc.apiplatformmicroservice.common.domain.services.NonEmptyListFormatters
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.services.ApplicationJsonFormatters

object SubscriptionFieldsConnectorDomain {
  import cats.data.{NonEmptyList => NEL}
  import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApiContext

  case class BulkSubscriptionFieldsResponse(subscriptions: Seq[SubscriptionFields])

  case class BulkApiFieldDefinitionsResponse(apis: Seq[ApiFieldDefinitions])

  type FieldErrors = Map[FieldName, String]

  case class SubscriptionFields(
      apiContext: ApiContext,
      apiVersion: ApiVersionNbr,
      fields: Map[FieldName, FieldValue]
    )

  case class ApiFieldDefinitions(apiContext: ApiContext, apiVersion: ApiVersionNbr, fieldDefinitions: NEL[FieldDefinition])

  case class SubscriptionFieldsPutRequest(
      clientId: ClientId,
      apiContext: ApiContext,
      apiVersion: ApiVersionNbr,
      fields: Map[FieldName, FieldValue]
    )

  case class ApplicationApiFieldValues(
      clientId: ClientId,
      apiContext: ApiContext,
      apiVersion: ApiVersionNbr,
      fieldsId: UUID,
      fields: Map[FieldName, FieldValue]
    )

  def asMapOfMapsOfFieldDefns(fieldDefs: Seq[ApiFieldDefinitions]): ApiFieldMap[FieldDefinition] = {
    import cats._
    import cats.implicits._
    type MapType = Map[ApiVersionNbr, Map[FieldName, FieldDefinition]]

    // Shortcut combining as we know there will never be records for the same version for the same context
    implicit def monoidVersions: Monoid[MapType] =
      new Monoid[MapType] {
        override def combine(x: MapType, y: MapType): MapType = x ++ y
        override def empty: MapType                           = Map.empty
      }

    Monoid.combineAll(
      fieldDefs.map(s => Map(s.apiContext -> Map(s.apiVersion -> s.fieldDefinitions.map(fd => fd.name -> fd).toList.toMap)))
    )
  }

  def asMapOfMaps(subscriptions: Seq[SubscriptionFields]): ApiFieldMap[FieldValue] = {
    import cats._
    import cats.implicits._
    type MapType = Map[ApiVersionNbr, Map[FieldName, FieldValue]]

    // Shortcut combining as we know there will never be records for the same version for the same context
    implicit def monoidVersions: Monoid[MapType] =
      new Monoid[MapType] {
        override def combine(x: MapType, y: MapType): MapType = x ++ y
        override def empty: MapType                           = Map.empty
      }

    Monoid.combineAll(
      subscriptions.map(s => Map(s.apiContext -> Map(s.apiVersion -> s.fields)))
    )
  }

  object JsonFormatters
      extends ApplicationJsonFormatters
      with FieldsJsonFormatters
      with NonEmptyListFormatters {

    import play.api.libs.json._
    import play.api.libs.functional.syntax._

    implicit val writeSubscriptionFields: OWrites[SubscriptionFields] = Json.writes[SubscriptionFields]

    implicit val writeSubscriptionFieldsPutRequest: Writes[SubscriptionFieldsPutRequest] = Json.writes[SubscriptionFieldsPutRequest]

    implicit val readsSubscriptionFields: Reads[SubscriptionFields] = (
      (JsPath \ "apiContext").read[ApiContext] and
        (JsPath \ "apiVersion").read[ApiVersionNbr] and
        (JsPath \ "fields").read[Map[FieldName, FieldValue]]
    )(SubscriptionFields.apply _)

    implicit val readsBulkSubscriptionFieldsResponse: Reads[BulkSubscriptionFieldsResponse] = Json.reads[BulkSubscriptionFieldsResponse]

    implicit val readsApplicationApiFieldValues: Reads[ApplicationApiFieldValues] = Json.reads[ApplicationApiFieldValues]

    implicit val readsFieldDefinitions: Reads[NEL[FieldDefinition]] = nelReads[FieldDefinition]

    implicit val readsApiFieldDefinitions: Reads[ApiFieldDefinitions] = Json.reads[ApiFieldDefinitions]

    implicit val readsBulkApiFieldDefinitionsResponse: Reads[BulkApiFieldDefinitionsResponse] = Json.reads[BulkApiFieldDefinitionsResponse]
  }
}
