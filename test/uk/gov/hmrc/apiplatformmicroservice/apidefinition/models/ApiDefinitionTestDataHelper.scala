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

import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.APIStatus.{STABLE}

trait ApiDefinitionTestDataHelper {
  def apiDefinition(
      name: String,
      versions: Seq[APIVersion] = Seq(apiVersion("1.0", STABLE))) = {
    APIDefinition(name, name, name, name, None, None, versions)
  }

  def apiAccess() = {
    APIAccess(
      `type` = APIAccessType.PUBLIC,
      whitelistedApplicationIds = Some(Seq.empty),
      isTrial = Some(false)
    )
  }

  implicit class ApiDefintionModifier(val inner: APIDefinition) {
    def requiresTrust(is: Option[Boolean]): APIDefinition =
      inner.copy(requiresTrust = is)

    def doesRequireTrust(): APIDefinition = requiresTrust(Some(true))
    def doesNotRequireTrust(): APIDefinition = requiresTrust(Some(false))
    def trustNotSpecified(): APIDefinition = requiresTrust(None)

    def withName(name: String): APIDefinition = inner.copy(name = name)
  }

  implicit class ApiAccessModifier(val inner: APIAccess) {
    def asPublic: APIAccess = {
      inner.copy(`type` = APIAccessType.PUBLIC)
    }
    def asPrivate: APIAccess = {
      inner.copy(`type` = APIAccessType.PRIVATE)
    }
    def asTrial: APIAccess = {
      inner.copy(isTrial = Some(true))
    }
    def notTrial: APIAccess = {
      inner.copy(isTrial = Some(false))
    }
    def withWhitelistedAppIds(appId: String*): APIAccess = {
      inner.copy(whitelistedApplicationIds = Some(appId.toSeq))
    }
  }

  def endpoint(endpointName: String = "Hello World",
               url: String = "/world"): Endpoint = {
    Endpoint(endpointName, url, HttpMethod.GET, None)
  }

  implicit class EndpointModifier(val inner: Endpoint) {
    def asPost: Endpoint =
      inner.copy(method = HttpMethod.POST)
  }

  def apiVersion(version: String = "1.0",
                 status: APIStatus = STABLE,
                 access: Option[APIAccess] = Some(apiAccess())): APIVersion = {
    APIVersion(version,
                status,
                access,
               Seq(endpoint("Today's Date", "/today"),
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
      inner.copy(access = inner.access.map(_.asPublic))

    def asPrivate: APIVersion =
      inner.copy(access = inner.access.map(_.asPrivate))

    def asTrial: APIVersion =
      inner.copy(access = inner.access.map(_.asTrial))

    def notTrial: APIVersion =
      inner.copy(access = inner.access.map(_.notTrial))

    def withAccess(altAccess: Option[APIAccess]): APIVersion =
      inner.copy(access = altAccess)

    def withNoAccess: APIVersion =
      inner.copy(access = None)
  }

}
