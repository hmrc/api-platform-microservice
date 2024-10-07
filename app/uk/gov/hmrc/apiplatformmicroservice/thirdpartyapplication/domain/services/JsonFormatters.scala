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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.services

import java.time.Instant

import play.api.libs.json._

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.TermsOfUseAgreement

trait ApplicationJsonFormatters extends EnvReads with EnvWrites {
  import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.controllers.domain.AddCollaboratorRequestOld

  object TOUAHelper {
    // DO NOT POLLUTE WHOLE SCOPE WITH THIS WRITER
    import uk.gov.hmrc.apiplatform.modules.common.domain.services.InstantJsonFormatter.lenientInstantReads
    implicit val formatDateTime: Format[Instant] = Format(lenientInstantReads, InstantEpochMilliWrites)
    val formatTOUA: Format[TermsOfUseAgreement]  = Json.format[TermsOfUseAgreement]
  }

  implicit val formatTermsOfUseAgreement: Format[TermsOfUseAgreement] = TOUAHelper.formatTOUA

  implicit val formatAddCollaboratorRequest: OFormat[AddCollaboratorRequestOld] = Json.format[AddCollaboratorRequestOld]
}

object ApplicationJsonFormatters extends ApplicationJsonFormatters
