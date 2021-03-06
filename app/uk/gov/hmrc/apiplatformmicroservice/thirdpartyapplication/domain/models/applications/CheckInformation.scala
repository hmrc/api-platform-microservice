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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications

import org.joda.time.DateTime

case class ContactDetails(fullname: String, email: String, telephoneNumber: String)

case class TermsOfUseAgreement(emailAddress: String, timeStamp: DateTime, version: String)

case class CheckInformation(
    confirmedName: Boolean = false,
    apiSubscriptionsConfirmed: Boolean = false,
    apiSubscriptionConfigurationsConfirmed: Boolean = false,
    contactDetails: Option[ContactDetails] = None,
    providedPrivacyPolicyURL: Boolean = false,
    providedTermsAndConditionsURL: Boolean = false,
    teamConfirmed: Boolean = false,
    termsOfUseAgreements: List[TermsOfUseAgreement] = List.empty)
