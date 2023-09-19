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

package uk.gov.hmrc.apiplatformmicroservice.apidefinition.models

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models._

trait ApiDefinitionTestDataHelper {

  def extendedApiDefinition(name: String, versions: List[ExtendedAPIVersion] = List(extendedApiVersion(ApiVersionNbr("1.0"), ApiStatus.STABLE))) = {
    ExtendedAPIDefinition(name, name, name, name, ApiContext(name), false, false, versions, List.empty, None)
  }

  def extendedApiVersion(
      version: ApiVersionNbr = ApiVersionNbr("1.0"),
      status: ApiStatus = ApiStatus.STABLE,
      productionAvailability: Option[ApiAvailability] = None,
      sandboxAvailability: Option[ApiAvailability] = None
    ): ExtendedAPIVersion = {
    ExtendedAPIVersion(version, status, List(endpoint("Today's Date", "/today"), endpoint("Yesterday's Date", "/yesterday")), productionAvailability, sandboxAvailability)
  }

  def apiDefinition(name: String): ApiDefinition = apiDefinition(name, apiVersion(ApiVersionNbr("1.0"), ApiStatus.STABLE))

  def apiDefinition(
      name: String,
      versions: ApiVersion*
    ) = {
    ApiDefinition(name, s"Urlof$name", name, name, ApiContext(name), versions.toList, false, false, None, List.empty)
  }

  def apiAccess() = {
    ApiAccess.PUBLIC
  }

  implicit class ApiDefintionModifier(val inner: ApiDefinition) {

    def isTestSupport(): ApiDefinition = inner.copy(isTestSupport = true)

    def requiresTrust(is: Boolean): ApiDefinition =
      inner.copy(requiresTrust = is)

    def withClosedAccess: ApiDefinition = inner.copy(versions = inner.versions.map(_.withClosedAccess))

    def asPrivate: ApiDefinition = inner.copy(versions = inner.versions.map(_.asPrivate))

    def doesRequireTrust: ApiDefinition    = requiresTrust(true)
    def doesNotRequireTrust: ApiDefinition = requiresTrust(false)
    def trustNotSpecified: ApiDefinition   = requiresTrust(false)

    def withName(name: String): ApiDefinition = inner.copy(name = name)

    def withVersions(versions: ApiVersion*): ApiDefinition = inner.copy(versions = versions.toList)

    def withCategories(categories: List[ApiCategory]): ApiDefinition = inner.copy(categories = categories)

    def asTrial: ApiDefinition = {
      inner.copy(versions = inner.versions.map(_.asTrial))
    }

    def asAlpha: ApiDefinition =
      inner.copy(versions = inner.versions.map(_.asAlpha))

    def asBeta: ApiDefinition =
      inner.copy(versions = inner.versions.map(_.asBeta))

    def asStable: ApiDefinition =
      inner.copy(versions = inner.versions.map(_.asStable))

    def asDeprecated: ApiDefinition =
      inner.copy(versions = inner.versions.map(_.asDeprecated))

    def asRetired: ApiDefinition =
      inner.copy(versions = inner.versions.map(_.asRetired))

  }

  implicit class ApiAccessPrivateModifier(val inner: ApiAccess.Private) {

    def asTrial: ApiAccess = {
      inner.copy(isTrial = true)
    }

    def notTrial: ApiAccess = {
      inner.copy(isTrial = false)
    }

    def withAllowlistedAppIds(appIds: ApplicationId*): ApiAccess = {
      inner.copy(whitelistedApplicationIds = appIds.toList.map(_.value.toString()))
    }

    def addAllowList(appIds: ApplicationId*): ApiAccess = {
      inner.copy(whitelistedApplicationIds = inner.whitelistedApplicationIds ++ appIds.toList.map(_.value.toString()))
    }
  }

  def endpoint(endpointName: String = "Hello World", url: String = "/world"): Endpoint = {
    Endpoint(endpointName, url, HttpMethod.GET, AuthType.NONE, ResourceThrottlingTier.UNLIMITED, None, List.empty)
  }

  implicit class EndpointModifier(val inner: Endpoint) {

    def asPost: Endpoint = inner.copy(method = HttpMethod.POST)

    def asUserRestricted: Endpoint = inner.copy(authType = AuthType.USER)

    def asApplicationRestricted: Endpoint = inner.copy(authType = AuthType.APPLICATION)
  }

  def apiVersion(version: ApiVersionNbr = ApiVersionNbr("1.0"), status: ApiStatus = ApiStatus.STABLE, access: ApiAccess = apiAccess()): ApiVersion = {
    ApiVersion(version, status, access, List(endpoint("Today's Date", "/today"), endpoint("Yesterday's Date", "/yesterday")), endpointsEnabled = false)
  }

  implicit class ApiVersionModifier(val inner: ApiVersion) {

    def asAlpha: ApiVersion =
      inner.copy(status = ApiStatus.ALPHA)

    def asBeta: ApiVersion =
      inner.copy(status = ApiStatus.BETA)

    def asStable: ApiVersion =
      inner.copy(status = ApiStatus.STABLE)

    def asDeprecated: ApiVersion =
      inner.copy(status = ApiStatus.DEPRECATED)

    def asRetired: ApiVersion =
      inner.copy(status = ApiStatus.RETIRED)

    def asPublic: ApiVersion =
      inner.copy(access = ApiAccess.PUBLIC)

    def asPrivate: ApiVersion =
      inner.copy(access = ApiAccess.Private(Nil, false))

    def asTrial: ApiVersion = inner.access match {
      case apiAccess: ApiAccess.Private => inner.copy(access = apiAccess.asTrial)
      case _                            => inner.copy(access = ApiAccess.Private(Nil, isTrial = true))
    }

    def addAllowList(applicationId: ApplicationId) =
      inner.access match {
        case p @ ApiAccess.Private(_, _) => inner.copy(access = p.addAllowList(applicationId))
        case _                           => inner
      }

    def notTrial: ApiVersion = inner.access match {
      case apiAccess: ApiAccess.Private => inner.copy(access = apiAccess.notTrial)
      case _                            => inner.copy(access = ApiAccess.Private(Nil, false))
    }

    def withAccess(altAccess: ApiAccess): ApiVersion =
      inner.copy(access = altAccess)

    def withClosedAccess: ApiVersion = inner.copy(endpoints = inner.endpoints.head.asApplicationRestricted :: inner.endpoints.tail)
  }

  implicit class ApiIdentifierSyntax(val context: String) {
    def asIdentifier(version: ApiVersionNbr): ApiIdentifier = ApiIdentifier(ApiContext(context), version)
    def asIdentifier(): ApiIdentifier                       = asIdentifier(ApiVersionNbr("1.0"))
  }

  implicit class ApiContextSyntax(val context: ApiContext) {
    def asIdentifier(version: ApiVersionNbr): ApiIdentifier = ApiIdentifier(context, version)
    def asIdentifier(): ApiIdentifier                       = asIdentifier(ApiVersionNbr("1.0"))
  }

  implicit class ApiVersionSyntax(val version: String) {
    def asVersion(): ApiVersionNbr = ApiVersionNbr(version)
  }
}
