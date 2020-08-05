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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.services

import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications._
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.common.domain.services.CommonJsonFormatters

trait JsonFormatters extends CommonJsonFormatters {
  import play.api.libs.json._
  import play.api.libs.json.JodaReads._
  import play.api.libs.json.JodaWrites._
  import play.api.libs.json.JodaWrites.JodaDateTimeNumberWrites
  import uk.gov.hmrc.play.json.Union

  implicit val formatApplicationId = Json.valueFormat[ApplicationId]
  implicit val formatClientId = Json.valueFormat[ClientId]

  val readsOverrideFlag = Reads[OverrideFlag] {
    case JsString(value) => JsSuccess(OverrideFlag(value))
    case o: JsObject     => Json.reads[OverrideFlag].reads(o)
    case _               => JsError()
  }

  val writesOverrideFlag = Json.writes[OverrideFlag]

  implicit val formatOverrideFlag = Format(readsOverrideFlag, writesOverrideFlag)

  implicit val formatStandard = Json.format[Standard]
  implicit val formatPrivileged = Json.format[Privileged]
  implicit val formatROPC = Json.format[ROPC]

  object TOUAHelper {
    // DO NOT POLLUTE WHOLE SCOPE WITH THIS WRITER
    implicit val formatDateTime = Format(DefaultJodaDateTimeReads, JodaDateTimeNumberWrites)
    val formatTOUA = Json.format[TermsOfUseAgreement]
  }

  implicit val formatTermsOfUseAgreement = TOUAHelper.formatTOUA

  implicit val formatCollaborator = Json.format[Collaborator]

  implicit val formatContactDetails = Json.format[ContactDetails]

  implicit val formatApplicationState = Json.format[ApplicationState]
  implicit val formatCheckInformation = Json.format[CheckInformation]

  implicit val formatAccessType = Union.from[Access]("accessType")
    .and[Standard](AccessType.STANDARD.toString)
    .and[Privileged](AccessType.PRIVILEGED.toString)
    .and[ROPC](AccessType.ROPC.toString)
    .format

  implicit val formatFieldName = Json.valueFormat[FieldName]
  implicit val formatFieldvalue = Json.valueFormat[FieldValue]

  implicit val keyReadsApiContext: KeyReads[ApiContext] = key => JsSuccess(ApiContext(key))
  implicit val keyWritesApiContext: KeyWrites[ApiContext] = _.value

  implicit val keyReadsApiVersion: KeyReads[ApiVersion] = key => JsSuccess(ApiVersion(key))
  implicit val keyWritesApiVersion: KeyWrites[ApiVersion] = _.value

  implicit val keyReadsFieldName: KeyReads[FieldName] = key => JsSuccess(FieldName(key))
  implicit val keyWritesFieldName: KeyWrites[FieldName] = _.value

  implicit val formatApplication = Json.format[Application]

  implicit val formatApplicationWithSubscriptionData = Json.format[ApplicationWithSubscriptionData]
}

object JsonFormatters extends JsonFormatters
