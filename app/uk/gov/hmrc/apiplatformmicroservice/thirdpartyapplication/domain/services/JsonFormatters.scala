/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.services

import play.api.libs.json.Json.JsValueWrapper
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.BasicApiDefinitionJsonFormatters
import uk.gov.hmrc.apiplatformmicroservice.common.domain.services.NonEmptyListFormatters

trait ApplicationJsonFormatters extends BasicApiDefinitionJsonFormatters {
  import play.api.libs.json._
  import play.api.libs.json.JodaReads._
  import play.api.libs.json.JodaWrites._
  import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications._
  import uk.gov.hmrc.play.json.Union
  import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.controllers.domain.AddCollaboratorRequest

  implicit val formatClientId = Json.valueFormat[ClientId]

  private implicit val formatGrantWithoutConsent = Json.format[GrantWithoutConsent]
  private implicit val formatPersistLogin = Format[PersistLogin](
    Reads { _ => JsSuccess(PersistLogin()) },
    Writes { _ => Json.obj() })
  private implicit val formatSuppressIvForAgents = Json.format[SuppressIvForAgents]
  private implicit val formatSuppressIvForOrganisations = Json.format[SuppressIvForOrganisations]
  private implicit val formatSuppressIvForIndividuals = Json.format[SuppressIvForIndividuals]

  implicit val formatOverrideType: Format[OverrideFlag] = Union.from[OverrideFlag]("overrideType")
    .and[GrantWithoutConsent](OverrideType.GRANT_WITHOUT_TAXPAYER_CONSENT.toString)
    .and[PersistLogin](OverrideType.PERSIST_LOGIN_AFTER_GRANT.toString)
    .and[SuppressIvForAgents](OverrideType.SUPPRESS_IV_FOR_AGENTS.toString)
    .and[SuppressIvForIndividuals](OverrideType.SUPPRESS_IV_FOR_INDIVIDUALS.toString)
    .and[SuppressIvForOrganisations](OverrideType.SUPPRESS_IV_FOR_ORGANISATIONS.toString)
    .format

  implicit val formatStandard = Json.format[Standard]
  implicit val formatPrivileged = Json.format[Privileged]
  implicit val formatROPC = Json.format[ROPC]

  object TOUAHelper {
    // DO NOT POLLUTE WHOLE SCOPE WITH THIS WRITER
    implicit val formatDateTime = Format(DefaultJodaDateTimeReads, JodaDateTimeNumberWrites)
    val formatTOUA: Format[TermsOfUseAgreement] = Json.format[TermsOfUseAgreement]
  }

  implicit val formatTermsOfUseAgreement = TOUAHelper.formatTOUA

  implicit val formatCollaborator: Format[Collaborator] = Json.format[Collaborator]

  implicit val formatContactDetails: Format[ContactDetails] = Json.format[ContactDetails]

  implicit val formatApplicationState: Format[ApplicationState] = Json.format[ApplicationState]
  implicit val formatCheckInformation: Format[CheckInformation] = Json.format[CheckInformation]

  implicit val formatAccessType: Format[Access] = Union.from[Access]("accessType")
    .and[Standard](AccessType.STANDARD.toString)
    .and[Privileged](AccessType.PRIVILEGED.toString)
    .and[ROPC](AccessType.ROPC.toString)
    .format

  implicit val formatIpAllowlist = Json.format[IpAllowlist]

  implicit val formatApplication: Format[Application] = Json.format[Application]

  implicit val formatFieldValue = Json.valueFormat[FieldValue]

  implicit val formatApplicationWithSubscriptionData = Json.format[ApplicationWithSubscriptionData]

  implicit val formatAddCollaboratorRequest = Json.format[AddCollaboratorRequest]

}

object ApplicationJsonFormatters extends ApplicationJsonFormatters

trait FieldsJsonFormatters extends BasicApiDefinitionJsonFormatters with NonEmptyListFormatters {
  import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.FieldName
  import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.fields._
  import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.fields.DevhubAccessRequirement._
  // TODO switch to easier Enumeratum
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
            List.empty[Option[(String, JsValueWrapper)]]
        ).filterNot(_.isEmpty).map(_.get): _*
      )
    }
  }
  implicit val readsAccessRequirements: Reads[AccessRequirements] = Json.reads[AccessRequirements]

  implicit val writesAccessRequirements: Writes[AccessRequirements] = Json.writes[AccessRequirements]

  implicit val formatValidationRule: OFormat[ValidationRule] = derived.withTypeTag.oformat(ShortClassName)

  implicit val formattValidationGroup = Json.format[ValidationGroup]

  implicit val readsFieldDefinitionType = Reads.enumNameReads(FieldDefinitionType)

  implicit val readsFieldDefinition: Reads[FieldDefinition] = (
    (JsPath \ "name").read[FieldName] and
      (JsPath \ "description").read[String] and
      ((JsPath \ "hint").read[String] or Reads.pure("")) and
      (JsPath \ "type").read[FieldDefinitionType.FieldDefinitionType] and
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
          (JsPath \ "type").write[FieldDefinitionType.FieldDefinitionType] and
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
