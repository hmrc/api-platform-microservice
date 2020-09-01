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

import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.{ApiDefinitionJsonFormatters, ApiVersion}
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.FieldName
import uk.gov.hmrc.apiplatformmicroservice.common.domain.services.{CommonJsonFormatters, NonEmptyListFormatters}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.services.{ApplicationJsonFormatters, FieldsJsonFormatters}

object SubscriptionFieldsConnectorDomain {
  import cats.data.{NonEmptyList => NEL}
  import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiContext
  import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.FieldValue
  import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.fields._

  case class SubscriptionFields(
      apiContext: ApiContext,
      apiVersion: ApiVersion,
      fields: Map[FieldName, FieldValue])

  case class BulkSubscriptionFieldsResponse(subscriptions: Seq[SubscriptionFields])

  case class ApiFieldDefinitions(apiContext: ApiContext, apiVersion: ApiVersion, fieldDefinitions: NEL[FieldDefinition])

  case class BulkApiFieldDefinitionsResponse(apis: Seq[ApiFieldDefinitions])

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

    for {
      fds <- fieldDefs
      fd <- fds.fieldDefinitions.toList
      a = fd.access
      _ = println(a)
    } yield ()

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

  trait SubscriptionFieldValuesJsonFormatters extends ApplicationJsonFormatters with CommonJsonFormatters {
    import play.api.libs.json._

    implicit val writeSubscriptionFields: OWrites[SubscriptionFields] = Json.writes[SubscriptionFields]

    import play.api.libs.functional.syntax._

    implicit val readsSubscriptionFields: Reads[SubscriptionFields] = (
      ((JsPath \ "apiContext").read[ApiContext]) and
        ((JsPath \ "apiVersion").read[ApiVersion]) and
        ((JsPath \ "fields").read[Map[FieldName, FieldValue]])
    )(SubscriptionFields.apply _)

    implicit val readsBulkSubscriptionFieldsResponse: Reads[BulkSubscriptionFieldsResponse] = Json.reads[BulkSubscriptionFieldsResponse]
  }

  object SubscriptionFieldValuesJsonFormatters extends SubscriptionFieldValuesJsonFormatters

  trait SubscriptionFieldDefinitionJsonFormatters extends ApiDefinitionJsonFormatters with ApplicationJsonFormatters with FieldsJsonFormatters with NonEmptyListFormatters {
    import play.api.libs.json._

    implicit val x: Reads[NEL[FieldDefinition]] = nelReads[FieldDefinition]
    implicit val readsApiFieldDefinitions: Reads[ApiFieldDefinitions] = Json.reads[ApiFieldDefinitions]

    implicit val readsBulkApiFieldDefinitionsResponse: Reads[BulkApiFieldDefinitionsResponse] = Json.reads[BulkApiFieldDefinitionsResponse]
  }

  object SubscriptionFieldDefinitionJsonFormatters extends SubscriptionFieldDefinitionJsonFormatters
}
