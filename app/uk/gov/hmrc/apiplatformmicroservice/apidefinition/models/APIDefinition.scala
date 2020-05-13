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

import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.APICategory.APICategory
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.APIStatus.APIStatus
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.HttpMethod.HttpMethod

case class APIDefinition(serviceName: String,
                         name: String,
                         description: String,
                         context: String,
                         requiresTrust: Option[Boolean],
                         isTestSupport: Option[Boolean] = None,
                         versions: Seq[APIVersion],
                         categories: Option[Seq[APICategory]] = None)

object APICategory extends Enumeration {
  type APICategory = Value

  val EXAMPLE, AGENTS, BUSINESS_RATES, CHARITIES, CONSTRUCTION_INDUSTRY_SCHEME, CORPORATION_TAX, CUSTOMS, ESTATES, HELP_TO_SAVE, INCOME_TAX_MTD,
  LIFETIME_ISA, MARRIAGE_ALLOWANCE, NATIONAL_INSURANCE, PAYE, PENSIONS, PRIVATE_GOVERNMENT,
  RELIEF_AT_SOURCE, SELF_ASSESSMENT, STAMP_DUTY, TRUSTS, VAT, VAT_MTD, OTHER = Value
}

case class APIVersion(version: String,
                      status: APIStatus,
                      access: Option[APIAccess],
                      endpoints: Seq[Endpoint])

object APIStatus extends Enumeration {
  type APIStatus = Value
  val PROTOTYPED, PUBLISHED, ALPHA, BETA, STABLE, DEPRECATED, RETIRED = Value
}

object APIAccessType extends Enumeration {
  type APIAccessType = Value
  val PRIVATE, PUBLIC = Value
}

case class APIAccess(`type`: APIAccessType.Value, whitelistedApplicationIds: Option[Seq[String]] = None, isTrial: Option[Boolean] = None)

case class Endpoint(endpointName: String,
                    uriPattern: String,
                    method: HttpMethod,
                    queryParameters: Option[Seq[Parameter]] = None)

object HttpMethod extends Enumeration {
  type HttpMethod = Value
  val GET, POST, PUT, PATCH, DELETE, OPTIONS = Value
}

case class Parameter(name: String, required: Boolean = false)
