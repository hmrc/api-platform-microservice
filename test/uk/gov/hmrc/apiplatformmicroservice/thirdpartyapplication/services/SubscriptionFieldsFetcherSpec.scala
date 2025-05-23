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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services

import scala.concurrent.ExecutionContext.Implicits.global

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.subscriptionfields.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.ThreeDMap
import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks.SubscriptionFieldsConnectorModule

class SubscriptionFieldsFetcherSpec extends AsyncHmrcSpec with SubscriptionFieldsConnectorModule {
  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val context1 = ApiContext("C1")
  private val context2 = ApiContext("C2")
  private val context3 = ApiContext("C3")
  private val context4 = ApiContext("C4")

  private val version1 = ApiVersionNbr("V1")
  private val version2 = ApiVersionNbr("V2")

  private val fieldName1 = FieldName("F1")
  private val fieldName2 = FieldName("F2")
  private val fieldName3 = FieldName("F3")

  def fieldDef(c: Int, v: Int, f: Int) = FieldDefinition(FieldName(s"F$c-$v-$f"), s"field $f", "", FieldDefinitionType.STRING, s"short $f", None)

  def fv(c: Int, v: Int, f: Int) = FieldValue(s"$c-$v-$f")

  private val defns: ApiFieldMap[FieldDefinition] = Map(
    context1 -> Map(
      version1 -> Map(
        fieldName1 -> fieldDef(1, 1, 1),
        fieldName2 -> fieldDef(1, 1, 2)
      ),
      version2 -> Map(
        fieldName1 -> fieldDef(1, 2, 1),
        fieldName2 -> fieldDef(1, 2, 2)
      )
    ),
    context2 -> Map(
      version1 -> Map(
        fieldName1 -> fieldDef(2, 1, 1)
      ),
      version2 -> Map(
        fieldName1 -> fieldDef(2, 2, 1),
        fieldName2 -> fieldDef(2, 2, 2)
      )
    ),
    context3 -> Map(
      version1 -> Map(
        fieldName1 -> fieldDef(3, 1, 1),
        fieldName2 -> fieldDef(3, 1, 2),
        fieldName3 -> fieldDef(3, 1, 3)
      )
    ),
    context4 -> Map(
      version1 -> Map(
        fieldName1 -> fieldDef(4, 1, 1),
        fieldName2 -> fieldDef(4, 1, 2),
        fieldName3 -> fieldDef(4, 1, 3)
      )
    )
  )

  private val values: ApiFieldMap[FieldValue] = Map(
    context1 -> Map(
      version1 -> Map(
        fieldName1 -> fv(1, 1, 1),
        fieldName2 -> fv(1, 1, 2)
      ),
      version2 -> Map(
        fieldName1 -> fv(1, 2, 1),
        fieldName2 -> fv(1, 2, 2)
      )
    ),
    context2 -> Map(
      version1 -> Map(
        fieldName1 -> fv(2, 1, 1)
      ),
      version2 -> Map(
        fieldName1 -> fv(2, 2, 1),
        fieldName2 -> fv(2, 2, 2)
      )
    ),
    context3 -> Map(
      version1 -> Map(
        fieldName1 -> fv(3, 1, 1),
        fieldName2 -> fv(3, 1, 2)
      )
    )
  )

  case class Setup(defns: ApiFieldMap[FieldDefinition], values: ApiFieldMap[FieldValue]) {
    SubordinateSubscriptionFieldsConnectorMock.BulkFetchFieldValues.willReturnFields(values)
    SubordinateSubscriptionFieldsConnectorMock.BulkFetchFieldDefinitions.willReturnDefinitions(defns)

    val fetcher = new SubscriptionFieldsService(EnvironmentAwareSubscriptionFieldsConnectorMock.instance)
  }

  def keys3[X, Y, Z, V](in: ThreeDMap.Type[X, Y, Z, V]): Set[(X, Y, Z)] = {
    in.flatMap(tupleXY =>
      tupleXY._2.flatMap(tupleYZ =>
        tupleYZ._2.map {
          case (z, v) => (tupleXY._1, tupleYZ._1, z)
        }
      )
    )
      .toSet
  }

  def contexts[Y, Z, V](in: ThreeDMap.Type[ApiContext, Y, Z, V]): Set[ApiContext] =
    in.keys.toSet

  def versions[Z, V](in: ThreeDMap.Type[ApiContext, ApiVersionNbr, Z, V]): Set[(ApiContext, ApiVersionNbr)] = {
    in.flatMap(tupleXY =>
      tupleXY._2.keys.map(y => (tupleXY._1, y))
    )
      .toSet
  }

  def flattenOut[X, Y, Z, V](in: ThreeDMap.Type[X, Y, Z, V]): Set[(X, Y, Z, V)] = {
    in.flatMap(tupleXY =>
      tupleXY._2.flatMap(tupleYZ =>
        tupleYZ._2.map {
          case (z, v) => (tupleXY._1, tupleYZ._1, z, v)
        }
      )
    )
      .toSet
  }

  "SubscriptionFieldsService" should {
    "fetch values where field values match field definitions" in new Setup(defns, values) {
      val subs: Set[ApiIdentifier] = Set(
        ApiIdentifier(context1, version1),
        ApiIdentifier(context2, version1)
      )

      val result = await(fetcher.fetchFieldValuesWithDefaults(Environment.SANDBOX, ClientId("1"), subs))

      result.keys should contain.allOf(context1, context2)

      flattenOut(result) should contain.allOf(
        (context1, version1, fieldName1, fv(1, 1, 1)),
        (context1, version1, fieldName2, fv(1, 1, 2)),
        (context2, version1, fieldName1, fv(2, 1, 1))
      )

      // Not subscribed to these contexts
      contexts(result) should contain.noneOf(context3, context4)

      // Not subscribed to these versions
      versions(result) should contain.noneOf(
        (context1, version2),
        (context2, version2)
      )
    }

    "fetch value where field definitions are missing field values" in new Setup(defns, values) {
      val subs: Set[ApiIdentifier] = Set(
        ApiIdentifier(context1, version1),
        ApiIdentifier(context2, version1),
        ApiIdentifier(context3, version1)
      )

      val result = await(fetcher.fetchFieldValuesWithDefaults(Environment.SANDBOX, ClientId("1"), subs))

      // Subscribed to contexts
      contexts(result) should contain.allOf(context1, context2, context3)

      // Note the blank field for absent field value
      flattenOut(result) should contain.allOf(
        (context1, version1, fieldName1, fv(1, 1, 1)),
        (context1, version1, fieldName2, fv(1, 1, 2)),
        (context2, version1, fieldName1, fv(2, 1, 1)),
        (context3, version1, fieldName1, fv(3, 1, 1)),
        (context3, version1, fieldName2, fv(3, 1, 2)),
        (context3, version1, fieldName3, FieldValue(""))
      )

      // Not subscribed to these contexts
      contexts(result) should not contain (context4)

      // Not subscribed to these versions
      versions(result) should contain.noneOf(
        (context1, version2),
        (context2, version2)
      )
    }

    "fetch value where all field values are missing" in new Setup(defns, values) {
      val subs: Set[ApiIdentifier] = Set(
        ApiIdentifier(context4, version1)
      )

      val result = await(fetcher.fetchFieldValuesWithDefaults(Environment.SANDBOX, ClientId("1"), subs))

      // Subscribed to contexts
      contexts(result) should contain(context4)

      // Not subscribed to these contexts
      contexts(result) should contain.noneOf(context1, context2, context3)

      // Note the blank field for absent field value
      flattenOut(result) should contain.allOf(
        (context4, version1, fieldName1, FieldValue("")),
        (context4, version1, fieldName2, FieldValue("")),
        (context4, version1, fieldName3, FieldValue(""))
      )
    }
  }
}
