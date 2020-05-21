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

package uk.gov.hmrc.apiplatformmicroservice.apidefinition.models

import play.api.libs.json._
import play.api.libs.functional.syntax._
import cats.data.{NonEmptyList => NEL}

trait NonEmptyListFormatters {

  implicit def nelReads[A](implicit r: Reads[A]): Reads[NEL[A]] =
    Reads
      .of[List[A]]
      .collect(
        JsonValidationError("expected a NonEmptyList but got an empty list")
      ) {
        case head :: tail => NEL(head, tail)
      }

  implicit def nelWrites[A](implicit w: Writes[A]): Writes[NEL[A]] =
    Writes
      .of[List[A]]
      .contramap(_.toList)
}

trait EndpointJsonFormatters extends NonEmptyListFormatters {
  implicit val formatParameter = Json.format[Parameter]
  implicit val formatEndpoint = Json.format[Endpoint]
}

object FormatterHelper {

  implicit class PathAdditions(path: JsPath) {

    def writeNullableIterable[A <: Iterable[_]](implicit writes: Writes[A]): OWrites[A] =
      OWrites[A] { (a: A) =>
        if (a.isEmpty) Json.obj()
        else JsPath.createObj(path -> writes.writes(a))
      }
  }

}
trait ApiDefinitionJsonFormatters
    extends EndpointJsonFormatters {

  import FormatterHelper.PathAdditions
  implicit val APIAccessReads: Reads[APIAccess] = (
    (JsPath \ "type").read[APIAccessType] and
      ((JsPath \ "whitelistedApplicationIds").read[Seq[String]] or Reads.pure(Seq.empty[String])) and
      ((JsPath \ "isTrial").read[Boolean] or Reads.pure(false))
    )(APIAccess.apply _)

  implicit val APIAccessWrites: Writes[APIAccess] = (
    (JsPath \ "type").write[APIAccessType] and
      (JsPath \ "whitelistedApplicationIds").writeNullableIterable[Seq[String]] and
      (JsPath \ "isTrial").write[Boolean]
    )(unlift(APIAccess.unapply))

  implicit val APIVersionReads: Reads[APIVersion] = (
    (JsPath \ "version").read[String] and
      (JsPath \ "status").read[APIStatus] and
      ((JsPath \ "access").read[APIAccess] or Reads.pure(APIAccess(APIAccessType.PUBLIC))) and
      (JsPath \ "endpoints").read[NEL[Endpoint]]
    )(APIVersion.apply _)

  implicit val APIVersionWrites : Writes[APIVersion] = Json.writes[APIVersion]

  implicit val formatAPIDefinition = Json.format[APIDefinition]
}

trait JsonFormatters extends ApiDefinitionJsonFormatters

object JsonFormatters extends JsonFormatters
