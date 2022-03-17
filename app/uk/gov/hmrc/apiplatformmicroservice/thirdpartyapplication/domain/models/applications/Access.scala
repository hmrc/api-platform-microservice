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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications

import play.api.libs.json.Json

case class ImportantSubmissionData(
  organisationUrl: Option[String] = None,
  responsibleIndividual: ResponsibleIndividual,
  serverLocations: Set[ServerLocation],
  termsAndConditionsLocation: TermsAndConditionsLocation,
  privacyPolicyLocation: PrivacyPolicyLocation,
  termsOfUseAcceptances: List[TermsOfUseAcceptance]
)

object ImportantSubmissionData {
  implicit val format = Json.format[ImportantSubmissionData]
}

sealed trait OverrideFlag {
  val overrideType: OverrideType
}

case object PersistLogin extends OverrideFlag {
  val overrideType = OverrideType.PERSIST_LOGIN_AFTER_GRANT
}

case class SuppressIvForAgents(scopes: Set[String]) extends OverrideFlag {
  val overrideType = OverrideType.SUPPRESS_IV_FOR_AGENTS
}

case class SuppressIvForOrganisations(scopes: Set[String]) extends OverrideFlag {
  val overrideType = OverrideType.SUPPRESS_IV_FOR_ORGANISATIONS
}

case class GrantWithoutConsent(scopes: Set[String]) extends OverrideFlag {
  val overrideType = OverrideType.GRANT_WITHOUT_TAXPAYER_CONSENT
}

case class SuppressIvForIndividuals(scopes: Set[String]) extends OverrideFlag {
  val overrideType = OverrideType.SUPPRESS_IV_FOR_INDIVIDUALS
}

sealed trait Access {
  val accessType: AccessType
}

case class Standard(
  redirectUris: List[String] = List.empty,
  termsAndConditionsUrl: Option[String] = None,
  privacyPolicyUrl: Option[String] = None,
  overrides: Set[OverrideFlag] = Set.empty,
  sellResellOrDistribute: Option[SellResellOrDistribute] = None,
  importantSubmissionData: Option[ImportantSubmissionData] = None
) extends Access {
  override val accessType = AccessType.STANDARD
}


case class Privileged(scopes: Set[String] = Set.empty) extends Access {
  override val accessType = AccessType.PRIVILEGED
}

case class ROPC(scopes: Set[String] = Set.empty) extends Access {
  override val accessType = AccessType.ROPC
}
