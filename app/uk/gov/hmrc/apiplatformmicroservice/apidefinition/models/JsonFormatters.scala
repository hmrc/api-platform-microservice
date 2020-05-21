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
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.APIAccessType.{PRIVATE, PUBLIC}

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

trait ApiDefinitionJsonFormatters
    extends EndpointJsonFormatters {

  implicit val apiAccessReads: Reads[APIAccess] = (
    (JsPath \ "type").read[APIAccessType] and
      ((JsPath \ "whitelistedApplicationIds").read[Seq[String]] or Reads.pure(Seq.empty[String])) and
      ((JsPath \ "isTrial").read[Boolean] or Reads.pure(false)) tupled) map {
    case (PUBLIC, _, _) => PublicApiAccess()
    case (PRIVATE, whitelistedApplicationIds, isTrial) => PrivateApiAccess(whitelistedApplicationIds, isTrial)
  }

  implicit object apiAccessWrites extends Writes[APIAccess] {
    private val privApiWrites: OWrites[(APIAccessType, Seq[String], Boolean)] = (
      (JsPath \ "type").write[APIAccessType] and
        (JsPath \ "whitelistedApplicationIds").write[Seq[String]] and
        (JsPath \ "isTrial").write[Boolean]
      ).tupled

    override def writes(access: APIAccess) = access match {
      case _: PublicApiAccess => Json.obj("type" -> PUBLIC)
      case privateApi: PrivateApiAccess => privApiWrites.writes((PRIVATE, privateApi.whitelistedApplicationIds, privateApi.isTrial))
    }
  }

  implicit val APIVersionReads: Reads[APIVersion] =
    ((JsPath \ "version").read[String] and
      (JsPath \ "status").read[APIStatus] and
      (JsPath \ "access").readNullable[APIAccess] and
      (JsPath \ "endpoints").read[NEL[Endpoint]] tupled) map {
      case (version, status, None, endpoints) => APIVersion(version, status, PublicApiAccess(), endpoints)
      case (version, status, Some(access), endpoints) => APIVersion(version, status, access, endpoints)
    }

  implicit val APIVersionWrites : Writes[APIVersion] = Json.writes[APIVersion]

  implicit val formatAPIDefinition = Json.format[APIDefinition]
}

trait JsonFormatters extends ApiDefinitionJsonFormatters

object JsonFormatters extends JsonFormatters
