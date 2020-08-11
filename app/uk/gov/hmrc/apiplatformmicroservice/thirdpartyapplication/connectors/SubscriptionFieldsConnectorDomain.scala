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

import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiVersion

private[connectors] object SubscriptionFieldsConnectorDomain {

  import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiContext
  import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.{ClientId, FieldName, FieldValue}

  case class ApplicationApiFieldValues(
      clientId: ClientId,
      apiContext: ApiContext,
      apiVersion: ApiVersion,
      fieldsId: ju.UUID,
      fields: Map[FieldName, FieldValue])

  case class BulkSubscriptionFieldsResponse(subscriptions: Seq[SubscriptionFields])

  case class SubscriptionFields(apiContext: ApiContext, apiVersion: ApiVersion, fields: Map[FieldName, FieldValue])

  def asMapOfMaps(subscriptions: Seq[SubscriptionFields]): Map[ApiContext, Map[ApiVersion, Map[FieldName, FieldValue]]] = {
    import cats._
    import cats.implicits._

    // Shortcut combining as we know there will never be records for the same version for the same context
    implicit def monoidVersions: Monoid[Map[ApiVersion, Map[FieldName, FieldValue]]] =
      new Monoid[Map[ApiVersion, Map[FieldName, FieldValue]]] {

        override def combine(x: Map[ApiVersion, Map[FieldName, FieldValue]], y: Map[ApiVersion, Map[FieldName, FieldValue]]): Map[ApiVersion, Map[FieldName, FieldValue]] = x ++ y

        override def empty: Map[ApiVersion, Map[FieldName, FieldValue]] = Map.empty
      }

    Monoid.combineAll(
      subscriptions.map(s => Map(s.apiContext -> Map(s.apiVersion -> s.fields)))
    )
  }

  trait JsonFormatters {
    import play.api.libs.json.Json
    import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.services.JsonFormatters._
    implicit val readsApplicationApiFieldValues = Json.reads[ApplicationApiFieldValues]

    implicit val readsSubscriptionFields = Json.reads[SubscriptionFields]
    implicit val readsBulkSubscriptionFieldsResponse = Json.reads[BulkSubscriptionFieldsResponse]
  }

  object JsonFormatters extends JsonFormatters
}
