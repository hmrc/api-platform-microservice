/*
 * Copyright 2022 HM Revenue & Customs
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

import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.BasicApiDefinitionJsonFormatters

trait ApplicationJsonFormatters extends BasicApiDefinitionJsonFormatters {
  import play.api.libs.json._
  import play.api.libs.json.JodaReads._
  import play.api.libs.json.JodaWrites._
  import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications._
  import uk.gov.hmrc.play.json.Union
  import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.controllers.domain.AddCollaboratorRequest

  implicit val formatClientId = Json.valueFormat[ClientId]

  private implicit val formatGrantWithoutConsent = Json.format[GrantWithoutConsent]

  private implicit val readsPersistLogin: Reads[PersistLogin.type] = Reads { _ => JsSuccess(PersistLogin) }
  private implicit val writesPersistLogin: OWrites[PersistLogin.type] = new OWrites[PersistLogin.type] {
    def writes(pl: PersistLogin.type) = Json.obj()
  }

  private implicit val formatSuppressIvForAgents = Json.format[SuppressIvForAgents]
  private implicit val formatSuppressIvForOrganisations = Json.format[SuppressIvForOrganisations]
  private implicit val formatSuppressIvForIndividuals = Json.format[SuppressIvForIndividuals]

  implicit val formatOverrideType: Format[OverrideFlag] = Union.from[OverrideFlag]("overrideType")
    .and[GrantWithoutConsent](OverrideType.GRANT_WITHOUT_TAXPAYER_CONSENT.toString)
    .and[PersistLogin.type](OverrideType.PERSIST_LOGIN_AFTER_GRANT.toString)
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

  implicit val formatApplicationWithSubscriptionData = Json.format[ApplicationWithSubscriptionData]

  implicit val formatAddCollaboratorRequest = Json.format[AddCollaboratorRequest]
}

object ApplicationJsonFormatters extends ApplicationJsonFormatters

