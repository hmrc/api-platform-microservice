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

import uk.gov.hmrc.apiplatform.modules.common.domain.models.*
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.*
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.Endpoint.UriPattern

trait ApiDefinitionTestDataHelper {

  def extendedApiDefinition(name: String, versions: List[ExtendedApiVersion] = List(extendedApiVersion(ApiVersionNbr("1.0"), ApiStatus.Stable))) = {
    ExtendedApiDefinition(
      serviceName = ServiceName(name),
      serviceBaseUrl = ApiDefinition.ServiceBaseUrl(name),
      name = ApiDefinition.Name(name),
      description = ApiDefinition.Description(name),
      context = ApiContext(name),
      versions = versions,
      isTestSupport = false,
      lastPublishedAt = None,
      categories = List.empty
    )
  }

  def extendedApiVersion(
      version: ApiVersionNbr = ApiVersionNbr("1.0"),
      status: ApiStatus = ApiStatus.Stable,
      productionAvailability: Option[ApiAvailability] = None,
      sandboxAvailability: Option[ApiAvailability] = None
    ): ExtendedApiVersion = {
    ExtendedApiVersion(version, status, List(endpoint("Today's Date", "/today"), endpoint("Yesterday's Date", "/yesterday")), productionAvailability, sandboxAvailability)
  }

  def apiDefinition(name: String): ApiDefinition = apiDefinition(name, apiVersion(ApiVersionNbr("1.0"), ApiStatus.Stable))

  def apiDefinition(
      name: String,
      versions: ApiVersion*
    ) = {
    ApiDefinition(
      ServiceName(name),
      ApiDefinition.ServiceBaseUrl(s"Urlof$name"),
      name = ApiDefinition.Name(name),
      description = ApiDefinition.Description(name),
      ApiContext(name),
      versions.toList.groupBy(_.versionNbr).map { case (k, v) => (k -> v.head) },
      false,
      None,
      List.empty
    )
  }

  implicit class ApiDefintionModifier(val inner: ApiDefinition) {

    def isTestSupport(): ApiDefinition = inner.copy(isTestSupport = true)

    def withClosedAccess: ApiDefinition = inner.copy(versions = inner.versions.map { case (k, v) => k -> v.withClosedAccess })

    def asInternal: ApiDefinition   = inner.copy(versions = inner.versions.map { case (k, v) => k -> v.asInternal })
    def asControlled: ApiDefinition = inner.copy(versions = inner.versions.map { case (k, v) => k -> v.asControlled })

    def withName(name: String): ApiDefinition = inner.copy(name = ApiDefinition.Name(name))

    def withVersions(versions: ApiVersion*): ApiDefinition = inner.copy(versions = versions.groupBy(_.versionNbr).map { case (k, vs) => k -> vs.head })

    def withCategories(categories: List[ApiCategory]): ApiDefinition = inner.copy(categories = categories)

    def asAlpha: ApiDefinition =
      inner.copy(versions = inner.versions.map { case (k, v) => k -> v.asAlpha })

    def asBeta: ApiDefinition =
      inner.copy(versions = inner.versions.map { case (k, v) => k -> v.asBeta })

    def asStable: ApiDefinition =
      inner.copy(versions = inner.versions.map { case (k, v) => k -> v.asStable })

    def asDeprecated: ApiDefinition =
      inner.copy(versions = inner.versions.map { case (k, v) => k -> v.asDeprecated })

    def asRetired: ApiDefinition =
      inner.copy(versions = inner.versions.map { case (k, v) => k -> v.asRetired })

  }

  def endpoint(endpointName: String = "Hello World", url: String = "/world"): Endpoint = {
    Endpoint(UriPattern(url), Endpoint.Name(endpointName), HttpMethod.Get, AuthType.None, ResourceThrottlingTier.Unlimited, None, List.empty)
  }

  implicit class EndpointModifier(val inner: Endpoint) {

    def asPost: Endpoint = inner.copy(method = HttpMethod.Post)

    def asUserRestricted: Endpoint = inner.copy(authType = AuthType.User)

    def asApplicationRestricted: Endpoint = inner.copy(authType = AuthType.Application)
  }

  def apiVersion(version: ApiVersionNbr = ApiVersionNbr("1.0"), status: ApiStatus = ApiStatus.Stable, access: ApiAccessType = ApiAccessType.Public): ApiVersion = {
    ApiVersion(
      version,
      status,
      access,
      List(endpoint("/today", "Today's Date"), endpoint("Yesterday's Date", "/yesterday")),
      endpointsEnabled = false,
      None,
      ApiVersionSource.OAS
    )
  }

  implicit class ApiVersionModifier(val inner: ApiVersion) {

    def asAlpha: ApiVersion =
      inner.copy(status = ApiStatus.Alpha)

    def asBeta: ApiVersion =
      inner.copy(status = ApiStatus.Beta)

    def asStable: ApiVersion =
      inner.copy(status = ApiStatus.Stable)

    def asDeprecated: ApiVersion =
      inner.copy(status = ApiStatus.Deprecated)

    def asRetired: ApiVersion =
      inner.copy(status = ApiStatus.Retired)

    def asPublic: ApiVersion =
      inner.copy(access = ApiAccessType.Public)

    def asInternal: ApiVersion =
      inner.copy(access = ApiAccessType.Internal)

    def asControlled: ApiVersion =
      inner.copy(access = ApiAccessType.Controlled)

    def withAccess(altAccess: ApiAccessType): ApiVersion =
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
