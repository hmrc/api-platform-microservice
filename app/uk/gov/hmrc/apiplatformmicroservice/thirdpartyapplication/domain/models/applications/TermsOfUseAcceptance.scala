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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications

import java.time.Instant

import play.api.libs.json._

import uk.gov.hmrc.apiplatform.modules.applications.submissions.domain.models.{ResponsibleIndividual, SubmissionId}

case class TermsOfUseAcceptance(responsibleIndividual: ResponsibleIndividual, dateTime: Instant, submissionId: SubmissionId, submissionInstance: Int)

object TermsOfUseAcceptance {
  import uk.gov.hmrc.apiplatform.modules.common.domain.services.InstantJsonFormatter.WithTimeZone._
  implicit val format: OFormat[TermsOfUseAcceptance] = Json.format[TermsOfUseAcceptance]
}
