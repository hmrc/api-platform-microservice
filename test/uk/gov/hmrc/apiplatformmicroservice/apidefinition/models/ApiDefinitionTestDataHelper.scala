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

import cats.data.{NonEmptyList => NEL}
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.APIStatus.STABLE

trait ApiDefinitionTestDataHelper {
  def extendedApiDefinition(name: String,
                            versions: Seq[ExtendedAPIVersion] = Seq(extendedApiVersion("1.0", STABLE))) = {
    ExtendedAPIDefinition(name, name, name, name, false, false, versions)
  }

  def extendedApiVersion(version: String = "1.0",
                         status: APIStatus = STABLE,
                         productionAvailability: Option[APIAvailability] = None,
                         sandboxAvailability: Option[APIAvailability] = None): ExtendedAPIVersion = {
    ExtendedAPIVersion(version,
      status,
      NEL.of(endpoint("Today's Date", "/today"),
        endpoint("Yesterday's Date", "/yesterday")),
      productionAvailability,
      sandboxAvailability)
  }

  def apiDefinition(
      name: String,
      versions: Seq[APIVersion] = Seq(apiVersion("1.0", STABLE))) = {
    APIDefinition(name, name, name, name, false, false, versions)
  }

  def apiAccess() = {
    PublicApiAccess()
  }

  implicit class ApiDefintionModifier(val inner: APIDefinition) {
    def requiresTrust(is: Boolean): APIDefinition =
      inner.copy(requiresTrust = is)

    def doesRequireTrust(): APIDefinition = requiresTrust(true)
    def doesNotRequireTrust(): APIDefinition = requiresTrust(false)
    def trustNotSpecified(): APIDefinition = requiresTrust(false)

    def withName(name: String): APIDefinition = inner.copy(name = name)

    def withVersions(versions: Seq[APIVersion]): APIDefinition = inner.copy(versions = versions)
  }

  implicit class PrivateApiAccessModifier(val inner: PrivateApiAccess) {
    def asTrial: APIAccess = {
      inner.copy(isTrial = true)
    }
    def notTrial: APIAccess = {
      inner.copy(isTrial = false)
    }
    def withWhitelistedAppIds(appId: String*): APIAccess = {
      inner.copy(whitelistedApplicationIds = appId.toSeq)
    }
  }

  def endpoint(endpointName: String = "Hello World",
               url: String = "/world"): Endpoint = {
    Endpoint(endpointName, url, HttpMethod.GET, Seq.empty)
  }

  implicit class EndpointModifier(val inner: Endpoint) {
    def asPost: Endpoint =
      inner.copy(method = HttpMethod.POST)
  }

  def apiVersion(version: String = "1.0",
                 status: APIStatus = STABLE,
                 access: APIAccess = apiAccess()): APIVersion = {
    APIVersion(version,
                status,
                access,
               NEL.of(endpoint("Today's Date", "/today"),
                   endpoint("Yesterday's Date", "/yesterday")))
  }

  implicit class ApiVersionModifier(val inner: APIVersion) {
    def asAlpha: APIVersion =
      inner.copy(status = APIStatus.ALPHA)

    def asBeta: APIVersion =
      inner.copy(status = APIStatus.BETA)

    def asStable: APIVersion =
      inner.copy(status = APIStatus.STABLE)

    def asDeprecated: APIVersion =
      inner.copy(status = APIStatus.DEPRECATED)

    def asRETIRED: APIVersion =
      inner.copy(status = APIStatus.RETIRED)

    def asPublic: APIVersion =
      inner.copy(access = PublicApiAccess())

    def asPrivate: APIVersion =
      inner.copy(access = PrivateApiAccess())

    def asTrial: APIVersion = inner.access match {
      case apiAccess: PrivateApiAccess => inner.copy(access = apiAccess.asTrial)
      case _ => inner.copy(access = PrivateApiAccess(isTrial = true))
    }

    def notTrial: APIVersion = inner.access match {
      case apiAccess: PrivateApiAccess => inner.copy(access = apiAccess.notTrial)
      case _ => inner.copy(access = PrivateApiAccess())
    }

    def withAccess(altAccess: APIAccess): APIVersion =
      inner.copy(access = altAccess)
  }

}
