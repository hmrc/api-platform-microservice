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

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApiContext, ApiIdentifier, ApplicationId, UserId}
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

  def fetch(serviceName: String, developerId: Option[UserId])(implicit hc: HeaderCarrier): Future[Option[ExtendedAPIDefinition]] = {
    val NO_APPS: Future[Set[ApplicationId]] = successful(Set())
    val NO_APIS: Future[Set[ApiIdentifier]] = successful(Set())

    for {
      principalDefinition   <- principalDefinitionService.fetchDefinition(serviceName)
      subordinateDefinition <- subordinateDefinitionService.fetchDefinition(serviceName)
      applicationIds        <- developerId.fold(NO_APPS)(appIdsFetcher.fetch)
      subscriptions         <- developerId.fold(NO_APIS)(subscriptionsForCollaboratorFetcher.fetch)
    } yield createExtendedApiDefinition(principalDefinition, subordinateDefinition, applicationIds, subscriptions, developerId)
  }

  def fetchCached(serviceName: String, developerId: Option[UserId])(implicit hc: HeaderCarrier): Future[Option[ExtendedAPIDefinition]] = {
    val key = s"${serviceName}---${developerId.map(_.toString()).getOrElse("NONE")}"

    cache.getOrElseUpdate(key, cacheExpiry) {
      fetch(serviceName, developerId)
    }
  }

  private def createExtendedApiDefinition(
      maybePrincipalDefinition: Option[ApiDefinition],
      maybeSubordinateDefinition: Option[ApiDefinition],
      applicationIds: Set[ApplicationId],
      subscriptions: Set[ApiIdentifier],
      userId: Option[UserId]
    ): Option[ExtendedAPIDefinition] = {

    def toCombinedApiDefinition(
        apiDefinition: ApiDefinition,
        principalVersions: List[ApiVersion],
        subordinateVersions: List[ApiVersion]
      ): Option[ExtendedAPIDefinition] = {
      if (apiDefinition.requiresTrust) {
        None
      } else {
        val extendedVersions = createExtendedApiVersions(apiDefinition.context, principalVersions, subordinateVersions, applicationIds, subscriptions, userId)
        if (extendedVersions.isEmpty) {
          None
        } else {
          Some(ExtendedAPIDefinition(
            apiDefinition.serviceName,
            apiDefinition.serviceBaseUrl,
            apiDefinition.name,
            apiDefinition.description,
            apiDefinition.context,
            apiDefinition.requiresTrust,
            apiDefinition.isTestSupport,
            extendedVersions,
            apiDefinition.categories,
            apiDefinition.lastPublishedAt
          ))
        }
      }
    }

    (maybePrincipalDefinition, maybeSubordinateDefinition) match {
      case (Some(principalDefinition), None)                        =>
        toCombinedApiDefinition(principalDefinition, principalDefinition.versions, List.empty)
      case (None, Some(subordinateDefinition))                      =>
        toCombinedApiDefinition(subordinateDefinition, List.empty, subordinateDefinition.versions)
      case (Some(principalDefinition), Some(subordinateDefinition)) =>
        toCombinedApiDefinition(subordinateDefinition, principalDefinition.versions, subordinateDefinition.versions)
      case _                                                        => None
    }
  }

  private def createExtendedApiVersions(
      context: ApiContext,
      principalVersions: List[ApiVersion],
      subordinateVersions: List[ApiVersion],
      applicationIds: Set[ApplicationId],
      subscriptions: Set[ApiIdentifier],
      userId: Option[UserId]
    ): List[ExtendedAPIVersion] = {
    val allVersions = (principalVersions.map(_.versionNbr) ++ subordinateVersions.map(_.versionNbr)).distinct.sorted
    allVersions map { versionNbr =>
      combineVersion(context, principalVersions.find(_.versionNbr == versionNbr), subordinateVersions.find(_.versionNbr == versionNbr), applicationIds, subscriptions, userId)
    } filter { version =>
      version.status != ApiStatus.RETIRED
    }
  }

  private def combineVersion(
      context: ApiContext,
      maybePrincipalVersion: Option[ApiVersion],
      maybeSubordinateVersion: Option[ApiVersion],
      applicationIds: Set[ApplicationId],
      subscriptions: Set[ApiIdentifier],
      userId: Option[UserId]
    ): ExtendedAPIVersion = {

    (maybePrincipalVersion, maybeSubordinateVersion) match {
      case (Some(principalVersion), None)                     =>
        toExtendedApiVersion(principalVersion, availability(context, principalVersion, applicationIds, subscriptions, userId), None)
      case (None, Some(subordinateVersion))                   =>
        toExtendedApiVersion(subordinateVersion, None, availability(context, subordinateVersion, applicationIds, subscriptions, userId))
      case (Some(principalVersion), Some(subordinateVersion)) =>
        toExtendedApiVersion(
          subordinateVersion,
          availability(context, principalVersion, applicationIds, subscriptions, userId),
          availability(context, subordinateVersion, applicationIds, subscriptions, userId)
        )
      case (None, None)                                       =>
        throw new IllegalStateException("It's impossible to get here from the call site")
    }
  }

  private def toExtendedApiVersion(
      apiVersion: ApiVersion,
      productionAvailability: Option[ApiAvailability],
      sandboxAvailability: Option[ApiAvailability]
    ): ExtendedAPIVersion = {
    ExtendedAPIVersion(
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
      applicationIds: Set[ApplicationId],
      subscriptions: Set[ApiIdentifier],
      userId: Option[UserId]
    ): Option[ApiAvailability] = {
    version.access match {
      case ApiAccess.Private(isTrial) =>
        val authorised = subscriptions.contains(ApiIdentifier(context, version.versionNbr))
        Some(ApiAvailability(version.endpointsEnabled, ApiAccess.Private(isTrial), userId.isDefined, authorised))
      case _                                     => Some(ApiAvailability(version.endpointsEnabled, ApiAccess.PUBLIC, userId.isDefined, authorised = true))
    }
  }
}
