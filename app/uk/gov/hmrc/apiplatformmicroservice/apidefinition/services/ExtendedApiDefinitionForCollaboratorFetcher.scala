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

package uk.gov.hmrc.apiplatformmicroservice.apidefinition.services

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future.successful
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

import play.api.cache.AsyncCacheApi
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApiContext, ApiIdentifier, ApiVersionNbr, UserId}
import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.{ApplicationIdsForCollaboratorFetcher, SubscriptionsForCollaboratorFetcher}

@Singleton
class ExtendedApiDefinitionForCollaboratorFetcher @Inject() (
    principalDefinitionService: PrincipalApiDefinitionService,
    subordinateDefinitionService: SubordinateApiDefinitionService,
    appIdsFetcher: ApplicationIdsForCollaboratorFetcher,
    subscriptionsForCollaboratorFetcher: SubscriptionsForCollaboratorFetcher,
    cache: AsyncCacheApi
  )(implicit ec: ExecutionContext
  ) {

  val cacheExpiry: FiniteDuration = 5 seconds

  def fetch(serviceName: ServiceName, developerId: Option[UserId])(implicit hc: HeaderCarrier): Future[Option[ExtendedApiDefinition]] = {
    val NO_APIS: Future[Set[ApiIdentifier]] = successful(Set())

    for {
      principalDefinition   <- principalDefinitionService.fetchDefinition(serviceName)
      subordinateDefinition <- subordinateDefinitionService.fetchDefinition(serviceName)
      subscriptions         <- developerId.fold(NO_APIS)(subscriptionsForCollaboratorFetcher.fetch)
    } yield createExtendedApiDefinition(principalDefinition, subordinateDefinition, subscriptions, developerId)
  }

  def fetchCached(serviceName: ServiceName, developerId: Option[UserId])(implicit hc: HeaderCarrier): Future[Option[ExtendedApiDefinition]] = {
    val key = s"${serviceName}---${developerId.map(_.toString()).getOrElse("NONE")}"

    cache.getOrElseUpdate(key, cacheExpiry) {
      fetch(serviceName, developerId)
    }
  }

  private def createExtendedApiDefinition(
      maybePrincipalDefinition: Option[ApiDefinition],
      maybeSubordinateDefinition: Option[ApiDefinition],
      subscriptions: Set[ApiIdentifier],
      userId: Option[UserId]
    ): Option[ExtendedApiDefinition] = {

    def toCombinedApiDefinition(
        apiDefinition: ApiDefinition,
        principalVersions: Map[ApiVersionNbr, ApiVersion],
        subordinateVersions: Map[ApiVersionNbr, ApiVersion]
      ): Option[ExtendedApiDefinition] = {
      if (apiDefinition.requiresTrust) {
        None
      } else {
        val extendedVersions = createExtendedApiVersions(apiDefinition.context, principalVersions, subordinateVersions, subscriptions, userId)
        if (extendedVersions.isEmpty) {
          None
        } else {
          Some(ExtendedApiDefinition(
            apiDefinition.serviceName,
            apiDefinition.serviceBaseUrl,
            apiDefinition.name,
            apiDefinition.description,
            apiDefinition.context,
            extendedVersions,
            apiDefinition.requiresTrust,
            apiDefinition.isTestSupport,
            apiDefinition.lastPublishedAt,
            apiDefinition.categories
          ))
        }
      }
    }

    (maybePrincipalDefinition, maybeSubordinateDefinition) match {
      case (Some(principalDefinition), None)                        =>
        toCombinedApiDefinition(principalDefinition, principalDefinition.versions, Map.empty)
      case (None, Some(subordinateDefinition))                      =>
        toCombinedApiDefinition(subordinateDefinition, Map.empty, subordinateDefinition.versions)
      case (Some(principalDefinition), Some(subordinateDefinition)) =>
        toCombinedApiDefinition(subordinateDefinition, principalDefinition.versions, subordinateDefinition.versions)
      case _                                                        =>
        None
    }
  }

  private def createExtendedApiVersions(
      context: ApiContext,
      principalVersions: Map[ApiVersionNbr, ApiVersion],
      subordinateVersions: Map[ApiVersionNbr, ApiVersion],
      subscriptions: Set[ApiIdentifier],
      userId: Option[UserId]
    ): List[ExtendedApiVersion] = {
    val allVersions = principalVersions.keySet ++ subordinateVersions.keySet
    allVersions.map(versionNbr =>
      combineVersion(context, principalVersions.get(versionNbr), subordinateVersions.get(versionNbr), subscriptions, userId)
    )
      .filter(version => version.status != ApiStatus.RETIRED)
      .toList
      .sortBy(_.version)
  }

  private def combineVersion(
      context: ApiContext,
      maybePrincipalVersion: Option[ApiVersion],
      maybeSubordinateVersion: Option[ApiVersion],
      subscriptions: Set[ApiIdentifier],
      userId: Option[UserId]
    ): ExtendedApiVersion = {

    (maybePrincipalVersion, maybeSubordinateVersion) match {
      case (Some(principalVersion), None)                     =>
        toExtendedApiVersion(principalVersion, availability(context, principalVersion, subscriptions, userId), None)
      case (None, Some(subordinateVersion))                   =>
        toExtendedApiVersion(subordinateVersion, None, availability(context, subordinateVersion, subscriptions, userId))
      case (Some(principalVersion), Some(subordinateVersion)) =>
        toExtendedApiVersion(
          subordinateVersion,
          availability(context, principalVersion, subscriptions, userId),
          availability(context, subordinateVersion, subscriptions, userId)
        )
      case (None, None)                                       =>
        throw new IllegalStateException("It's impossible to get here from the call site")
    }
  }

  private def toExtendedApiVersion(
      apiVersion: ApiVersion,
      productionAvailability: Option[ApiAvailability],
      sandboxAvailability: Option[ApiAvailability]
    ): ExtendedApiVersion = {
    ExtendedApiVersion(
      version = apiVersion.versionNbr,
      status = apiVersion.status,
      endpoints = apiVersion.endpoints,
      productionAvailability = productionAvailability,
      sandboxAvailability = sandboxAvailability
    )
  }

  private def availability(
      context: ApiContext,
      version: ApiVersion,
      subscriptions: Set[ApiIdentifier],
      userId: Option[UserId]
    ): Option[ApiAvailability] = {
    version.access match {
      case ApiAccess.Private(isTrial) =>
        val authorised = subscriptions.contains(ApiIdentifier(context, version.versionNbr))
        Some(ApiAvailability(version.endpointsEnabled, ApiAccess.Private(isTrial), userId.isDefined, authorised))
      case _                          => Some(ApiAvailability(version.endpointsEnabled, ApiAccess.PUBLIC, userId.isDefined, authorised = true))
    }
  }
}
