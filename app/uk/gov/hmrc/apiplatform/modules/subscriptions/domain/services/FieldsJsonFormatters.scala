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

package uk.gov.hmrc.apiplatform.modules.subscriptions.domain.services

import uk.gov.hmrc.apiplatform.modules.subscriptions.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.common.domain.services.NonEmptyListFormatters

trait FieldsJsonFormatters extends NonEmptyListFormatters {
  import uk.gov.hmrc.apiplatform.modules.subscriptions.domain.models.DevhubAccessRequirement._

  import julienrf.json.derived
  import julienrf.json.derived.TypeTagSetting.ShortClassName
  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  def ignoreDefaultField[T](value: T, default: T, jsonFieldName: String)(implicit w: Writes[T]) =
    if (value == default) None else Some((jsonFieldName, Json.toJsFieldJsValueWrapper(value)))

  implicit val formatDevhubAccessRequirement: Format[DevhubAccessRequirement] = new Format[DevhubAccessRequirement] {

    override def writes(o: DevhubAccessRequirement): JsValue = JsString(o match {
      case AdminOnly => "adminOnly"
      case Anyone    => "anyone"
      case NoOne     => "noOne"
    })

    override def reads(json: JsValue): JsResult[DevhubAccessRequirement] = json match {
      case JsString("adminOnly") => JsSuccess(AdminOnly)
      case JsString("anyone")    => JsSuccess(Anyone)
      case JsString("noOne")     => JsSuccess(NoOne)
      case _                     => JsError("Not a recognized DevhubAccessRequirement")
    }
  }

  implicit val readsDevhubAccessRequirements: Reads[DevhubAccessRequirements] = (
    ((JsPath \ "read").read[DevhubAccessRequirement] or Reads.pure(DevhubAccessRequirement.Default)) and
      ((JsPath \ "write").read[DevhubAccessRequirement] or Reads.pure(DevhubAccessRequirement.Default))
  )(DevhubAccessRequirements.apply _)

  implicit val writesDevhubAccessRequirements: OWrites[DevhubAccessRequirements] = new OWrites[DevhubAccessRequirements] {

    def writes(requirements: DevhubAccessRequirements) = {
      Json.obj(
        (
          ignoreDefaultField(requirements.read, DevhubAccessRequirement.Default, "read") ::
            ignoreDefaultField(requirements.write, DevhubAccessRequirement.Default, "write") ::
            List.empty[Option[(String, Json.JsValueWrapper)]]
        ).filterNot(_.isEmpty).map(_.get): _*
      )
    }
  }
  implicit val readsAccessRequirements: Reads[AccessRequirements] = Json.reads[AccessRequirements]

  implicit val writesAccessRequirements: Writes[AccessRequirements] = Json.writes[AccessRequirements]

  implicit val formatValidationRule: OFormat[ValidationRule] = derived.withTypeTag.oformat[ValidationRule](ShortClassName)

  implicit val formattValidationGroup: OFormat[ValidationGroup] = Json.format[ValidationGroup]

  implicit val readsFieldDefinition: Reads[FieldDefinition] = (
    (JsPath \ "name").read[FieldName] and
      (JsPath \ "description").read[String] and
      ((JsPath \ "hint").read[String] or Reads.pure("")) and
      (JsPath \ "type").read[FieldDefinitionType] and
      ((JsPath \ "shortDescription").read[String] or Reads.pure("")) and
      (JsPath \ "validation").readNullable[ValidationGroup] and
      ((JsPath \ "access").read[AccessRequirements] or Reads.pure(AccessRequirements.Default))
  )(FieldDefinition.apply _)

  implicit val writesFieldDefinition: Writes[FieldDefinition] = new Writes[FieldDefinition] {

    def dropTail[A, B, C, D, E, F, G](t: Tuple7[A, B, C, D, E, F, G]): Tuple6[A, B, C, D, E, F] = (t._1, t._2, t._3, t._4, t._5, t._6)

    // This allows us to hide default AccessRequirements from JSON - as this is a rarely used field
    // but not one that business logic would want as an optional field and require getOrElse everywhere.
    override def writes(o: FieldDefinition): JsValue = {
      val common =
        (JsPath \ "name").write[FieldName] and
          (JsPath \ "description").write[String] and
          (JsPath \ "hint").write[String] and
          (JsPath \ "type").write[FieldDefinitionType] and
          (JsPath \ "shortDescription").write[String] and
          (JsPath \ "validation").writeNullable[ValidationGroup]

      (if (o.access == AccessRequirements.Default) {
         (common)(unlift(FieldDefinition.unapply).andThen(dropTail))
       } else {
         (common and (JsPath \ "access").write[AccessRequirements])(unlift(FieldDefinition.unapply))
       }).writes(o)
    }
  }
}

object FieldsJsonFormatters extends FieldsJsonFormatters
