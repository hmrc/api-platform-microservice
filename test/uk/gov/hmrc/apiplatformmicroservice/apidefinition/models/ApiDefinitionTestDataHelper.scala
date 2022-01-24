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

package uk.gov.hmrc.apiplatformmicroservice.apidefinition.models

import cats.data.{NonEmptyList => NEL}
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiStatus.STABLE
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.ApplicationId

trait ApiDefinitionTestDataHelper {

  def extendedApiDefinition(name: String, versions: List[ExtendedApiVersion] = List(extendedApiVersion(ApiVersion("1.0"), STABLE))) = {
    ExtendedApiDefinition(name, name, name, ApiContext(name), false, false, versions, List.empty)
  }

  def extendedApiVersion(
      version: ApiVersion = ApiVersion("1.0"),
      status: ApiStatus = STABLE,
      productionAvailability: Option[ApiAvailability] = None,
      sandboxAvailability: Option[ApiAvailability] = None
    ): ExtendedApiVersion = {
    ExtendedApiVersion(version, status, NEL.of(endpoint("Today's Date", "/today"), endpoint("Yesterday's Date", "/yesterday")), productionAvailability, sandboxAvailability)
  }

  def apiDefinition(name: String): ApiDefinition = apiDefinition(name, apiVersion(ApiVersion("1.0"), STABLE))

  def apiDefinition(
      name: String,
      versions: ApiVersionDefinition*
    ) = {
    ApiDefinition(name, name, name, ApiContext(name), false, false, versions.toList)
  }

  def apiAccess() = {
    PublicApiAccess()
  }

  implicit class ApiDefintionModifier(val inner: ApiDefinition) {

    def isTestSupport(): ApiDefinition = inner.copy(isTestSupport = true)

    def requiresTrust(is: Boolean): ApiDefinition =
      inner.copy(requiresTrust = is)

    def withClosedAccess: ApiDefinition = inner.copy(versions = inner.versions.map(_.withClosedAccess))

    def asPrivate: ApiDefinition = inner.copy(versions = inner.versions.map(_.asPrivate))
    
    def doesRequireTrust: ApiDefinition = requiresTrust(true)
    def doesNotRequireTrust: ApiDefinition = requiresTrust(false)
    def trustNotSpecified: ApiDefinition = requiresTrust(false)

    def withName(name: String): ApiDefinition = inner.copy(name = name)

    def withVersions(versions: ApiVersionDefinition*): ApiDefinition = inner.copy(versions = versions.toList)

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

  implicit class PrivateApiAccessModifier(val inner: PrivateApiAccess) {

    def asTrial: ApiAccess = {
      inner.copy(isTrial = true)
    }

    def notTrial: ApiAccess = {
      inner.copy(isTrial = false)
    }

    def withAllowlistedAppIds(appIds: ApplicationId*): ApiAccess = {
      inner.copy(allowlistedApplicationIds = appIds.toList)
    }

    def addAllowList(appIds: ApplicationId*): ApiAccess = {
      inner.copy(allowlistedApplicationIds = inner.allowlistedApplicationIds ++ appIds)
    }
  }

  def endpoint(endpointName: String = "Hello World", url: String = "/world"): Endpoint = {
    Endpoint(endpointName, url, HttpMethod.GET, AuthType.NONE, List.empty)
  }

  implicit class EndpointModifier(val inner: Endpoint) {

    def asPost: Endpoint = inner.copy(method = HttpMethod.POST)

    def asUserRestricted: Endpoint = inner.copy(authType = AuthType.USER)

    def asApplicationRestricted: Endpoint = inner.copy(authType = AuthType.APPLICATION)
  }

  def apiVersion(version: ApiVersion = ApiVersion("1.0"), status: ApiStatus = STABLE, access: ApiAccess = apiAccess()): ApiVersionDefinition = {
    ApiVersionDefinition(version, status, access, NEL.of(endpoint("Today's Date", "/today"), endpoint("Yesterday's Date", "/yesterday")))
  }

  implicit class ApiVersionModifier(val inner: ApiVersionDefinition) {

    def asAlpha: ApiVersionDefinition =
      inner.copy(status = ApiStatus.ALPHA)

    def asBeta: ApiVersionDefinition =
      inner.copy(status = ApiStatus.BETA)

    def asStable: ApiVersionDefinition =
      inner.copy(status = ApiStatus.STABLE)

    def asDeprecated: ApiVersionDefinition =
      inner.copy(status = ApiStatus.DEPRECATED)

    def asRetired: ApiVersionDefinition =
      inner.copy(status = ApiStatus.RETIRED)

    def asPublic: ApiVersionDefinition =
      inner.copy(access = PublicApiAccess())

    def asPrivate: ApiVersionDefinition =
      inner.copy(access = PrivateApiAccess())

    def asTrial: ApiVersionDefinition = inner.access match {
      case apiAccess: PrivateApiAccess => inner.copy(access = apiAccess.asTrial)
      case _                           => inner.copy(access = PrivateApiAccess(isTrial = true))
    }

    def addAllowList(applicationId: ApplicationId) = 
      inner.access match {
        case p @ PrivateApiAccess(_,_) => inner.copy(access = p.addAllowList(applicationId))
        case _ => inner
      }
    def notTrial: ApiVersionDefinition = inner.access match {
      case apiAccess: PrivateApiAccess => inner.copy(access = apiAccess.notTrial)
      case _                           => inner.copy(access = PrivateApiAccess())
    }

    def withAccess(altAccess: ApiAccess): ApiVersionDefinition =
      inner.copy(access = altAccess)

    def withClosedAccess: ApiVersionDefinition = inner.copy(endpoints = NEL(inner.endpoints.head.asApplicationRestricted, inner.endpoints.tail))
  }

  implicit class ApiIdentifierSyntax(val context: String) {
    def asIdentifier(version: ApiVersion): ApiIdentifier = ApiIdentifier(ApiContext(context), version)
    def asIdentifier(): ApiIdentifier = asIdentifier(ApiVersion("1.0"))
  }

  implicit class ApiContextSyntax(val context: ApiContext) {
    def asIdentifier(version: ApiVersion): ApiIdentifier = ApiIdentifier(context, version)
    def asIdentifier(): ApiIdentifier = asIdentifier(ApiVersion("1.0"))
  }

  implicit class ApiVersionSyntax(val version: String) {
    def asVersion(): ApiVersion = ApiVersion(version)
  }
}