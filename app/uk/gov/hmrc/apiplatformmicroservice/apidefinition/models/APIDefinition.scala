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

import enumeratum._
import cats.data.{NonEmptyList => NEL}
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.ApplicationId

case class ApiContext(value: String) extends AnyVal

object ApiContext {

  implicit val ordering: Ordering[ApiContext] = new Ordering[ApiContext] {
    override def compare(x: ApiContext, y: ApiContext): Int = x.value.compareTo(y.value)

  }
}

case class ApiVersion(value: String) extends AnyVal

object ApiVersion {

  implicit val ordering: Ordering[ApiVersion] = new Ordering[ApiVersion] {
    override def compare(x: ApiVersion, y: ApiVersion): Int = x.value.compareTo(y.value)
  }
}

case class ApiIdentifier(context: ApiContext, version: ApiVersion)

case class APIDefinition(
    serviceName: String,
    name: String,
    description: String,
    context: ApiContext,
    requiresTrust: Boolean = false,
    isTestSupport: Boolean = false,
    versions: Seq[ApiVersionDefinition],
    categories: Seq[APICategory] = Seq.empty) {

  def hasActiveVersions: Boolean = versions.exists(_.status != APIStatus.RETIRED)
}

sealed trait APICategory extends EnumEntry

object APICategory extends Enum[APICategory] with PlayJsonEnum[APICategory] {

  val values = findValues

  case object EXAMPLE extends APICategory
  case object AGENTS extends APICategory
  case object BUSINESS_RATES extends APICategory
  case object CHARITIES extends APICategory
  case object CONSTRUCTION_INDUSTRY_SCHEME extends APICategory
  case object CORPORATION_TAX extends APICategory
  case object CUSTOMS extends APICategory
  case object ESTATES extends APICategory
  case object HELP_TO_SAVE extends APICategory
  case object INCOME_TAX_MTD extends APICategory
  case object LIFETIME_ISA extends APICategory
  case object MARRIAGE_ALLOWANCE extends APICategory
  case object NATIONAL_INSURANCE extends APICategory
  case object PAYE extends APICategory
  case object PENSIONS extends APICategory
  case object PRIVATE_GOVERNMENT extends APICategory
  case object RELIEF_AT_SOURCE extends APICategory
  case object SELF_ASSESSMENT extends APICategory
  case object STAMP_DUTY extends APICategory
  case object TRUSTS extends APICategory
  case object VAT extends APICategory
  case object VAT_MTD extends APICategory
  case object OTHER extends APICategory

}

case class ApiVersionDefinition(version: ApiVersion, status: APIStatus, access: APIAccess, endpoints: NEL[Endpoint], endpointsEnabled: Boolean = false)

sealed trait APIStatus extends EnumEntry

object APIStatus extends Enum[APIStatus] with PlayJsonEnum[APIStatus] {

  val values = findValues

  case object PROTOTYPED extends APIStatus
  case object PUBLISHED extends APIStatus
  case object ALPHA extends APIStatus
  case object BETA extends APIStatus
  case object STABLE extends APIStatus
  case object DEPRECATED extends APIStatus
  case object RETIRED extends APIStatus
}

sealed trait APIAccessType extends EnumEntry

object APIAccessType extends Enum[APIAccessType] with PlayJsonEnum[APIAccessType] {

  val values = findValues

  case object PRIVATE extends APIAccessType
  case object PUBLIC extends APIAccessType
}

trait APIAccess
case class PublicApiAccess() extends APIAccess
case class PrivateApiAccess(whitelistedApplicationIds: Seq[ApplicationId] = Seq.empty, isTrial: Boolean = false) extends APIAccess

case class Endpoint(endpointName: String, uriPattern: String, method: HttpMethod, queryParameters: Seq[Parameter] = Seq.empty)

sealed trait HttpMethod extends EnumEntry

object HttpMethod extends Enum[HttpMethod] with PlayJsonEnum[HttpMethod] {

  val values = findValues

  case object GET extends HttpMethod
  case object POST extends HttpMethod
  case object PUT extends HttpMethod
  case object PATCH extends HttpMethod
  case object DELETE extends HttpMethod
  case object OPTIONS extends HttpMethod
}

case class Parameter(name: String, required: Boolean = false)
