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

package uk.gov.hmrc.apiplatformmicroservice.apidefinition.models

import cats.data.{NonEmptyList => NEL}
import enumeratum._
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.ApplicationId
import scala.util.Random

case class ApiContext(value: String) extends AnyVal

object ApiContext {
  implicit val ordering: Ordering[ApiContext] = new Ordering[ApiContext] {
    override def compare(x: ApiContext, y: ApiContext): Int = x.value.compareTo(y.value)
  }
   def random = ApiContext(Random.alphanumeric.take(10).mkString)
}

case class ApiVersion(value: String) extends AnyVal

object ApiVersion {
  implicit val ordering: Ordering[ApiVersion] = new Ordering[ApiVersion] {
    override def compare(x: ApiVersion, y: ApiVersion): Int = x.value.compareTo(y.value)
  }
  def random = ApiVersion(Random.nextDouble().toString)
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
    categories: Seq[APICategory] = Seq.empty)

case class APICategory(value: String) extends AnyVal

case class APICategoryDetails(category: String, name: String) {
  def asAPICategory(): APICategory = APICategory(category)
}

case class ApiVersionDefinition(version: ApiVersion, status: APIStatus, access: APIAccess, endpoints: NEL[Endpoint], endpointsEnabled: Boolean = false)

sealed trait APIStatus extends EnumEntry

object APIStatus extends Enum[APIStatus] with PlayJsonEnum[APIStatus] {

  val values = findValues
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

case class Endpoint(endpointName: String, uriPattern: String, method: HttpMethod, authType: AuthType, queryParameters: Seq[Parameter] = Seq.empty)

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

sealed trait AuthType extends EnumEntry

object AuthType extends Enum[AuthType] with PlayJsonEnum[AuthType] {

  val values = findValues

  case object NONE extends AuthType
  case object APPLICATION extends AuthType
  case object USER extends AuthType

}

case class Parameter(name: String, required: Boolean = false)
