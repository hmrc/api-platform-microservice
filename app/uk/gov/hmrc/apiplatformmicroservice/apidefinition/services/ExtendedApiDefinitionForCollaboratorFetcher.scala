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
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiStatus.RETIRED
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models._
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.ApplicationIdsForCollaboratorFetcher
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.SubscriptionsForCollaboratorFetcher
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.UserId

@Singleton
class ExtendedApiDefinitionForCollaboratorFetcher @Inject() (
    principalDefinitionService: PrincipalApiDefinitionService,
    subordinateDefinitionService: SubordinateApiDefinitionService,
    appIdsFetcher: ApplicationIdsForCollaboratorFetcher,
    subscriptionsForCollaboratorFetcher: SubscriptionsForCollaboratorFetcher
  )(implicit ec: ExecutionContext
  ) {

  def fetch(serviceName: String, developerId: Option[UserId])(implicit hc: HeaderCarrier): Future[Option[ExtendedApiDefinition]] = {
    for {
      principalDefinition   <- principalDefinitionService.fetchDefinition(serviceName)
      subordinateDefinition <- subordinateDefinitionService.fetchDefinition(serviceName)
      applicationIds        <- developerId.fold(successful(Set.empty[ApplicationId]))(appIdsFetcher.fetch)
      subscriptions         <- developerId.fold(successful(Set.empty[ApiIdentifier]))(subscriptionsForCollaboratorFetcher.fetch)
    } yield createExtendedApiDefinition(principalDefinition, subordinateDefinition, applicationIds, subscriptions, developerId)
  }

  private def createExtendedApiDefinition(
      maybePrincipalDefinition: Option[ApiDefinition],
      maybeSubordinateDefinition: Option[ApiDefinition],
      applicationIds: Set[ApplicationId],
      subscriptions: Set[ApiIdentifier],
      userId: Option[UserId]
    ): Option[ExtendedApiDefinition] = {

    def toCombinedApiDefinition(
        apiDefinition: ApiDefinition,
        principalVersions: List[ApiVersionDefinition],
        subordinateVersions: List[ApiVersionDefinition]
      ): Option[ExtendedApiDefinition] = {
      if (apiDefinition.requiresTrust) {
        None
      } else {
        val extendedVersions = createExtendedApiVersions(apiDefinition.context, principalVersions, subordinateVersions, applicationIds, subscriptions, userId)
        if (extendedVersions.isEmpty) {
          None
        } else {
          Some(ExtendedApiDefinition(
            apiDefinition.serviceName,
            apiDefinition.name,
            apiDefinition.description,
            apiDefinition.context,
            apiDefinition.requiresTrust,
            apiDefinition.isTestSupport,
            extendedVersions,
            apiDefinition.categories
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
      principalVersions: List[ApiVersionDefinition],
      subordinateVersions: List[ApiVersionDefinition],
      applicationIds: Set[ApplicationId],
      subscriptions: Set[ApiIdentifier],
      userId: Option[UserId]
    ): List[ExtendedApiVersion] = {
    val allVersions = (principalVersions.map(_.version) ++ subordinateVersions.map(_.version)).distinct.sorted
    allVersions map { version =>
      combineVersion(context, principalVersions.find(_.version == version), subordinateVersions.find(_.version == version), applicationIds, subscriptions, userId)
    } filter { version =>
      version.status != RETIRED
    }
  }

  private def combineVersion(
      context: ApiContext,
      maybePrincipalVersion: Option[ApiVersionDefinition],
      maybeSubordinateVersion: Option[ApiVersionDefinition],
      applicationIds: Set[ApplicationId],
      subscriptions: Set[ApiIdentifier],
      userId: Option[UserId]
    ): ExtendedApiVersion = {

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
      apiVersion: ApiVersionDefinition,
      productionAvailability: Option[ApiAvailability],
      sandboxAvailability: Option[ApiAvailability]
    ): ExtendedApiVersion = {
    ExtendedApiVersion(
      version = apiVersion.version,
      status = apiVersion.status,
      endpoints = apiVersion.endpoints,
      productionAvailability = productionAvailability,
      sandboxAvailability = sandboxAvailability
    )
  }

  private def availability(
      context: ApiContext,
      version: ApiVersionDefinition,
      applicationIds: Set[ApplicationId],
      subscriptions: Set[ApiIdentifier],
      userId: Option[UserId]
    ): Option[ApiAvailability] = {
    version.access match {
      case PrivateApiAccess(allowlist, isTrial) =>
        val authorised = applicationIds.intersect(allowlist.toSet).nonEmpty || subscriptions.contains(ApiIdentifier(context, version.version))
        Some(ApiAvailability(version.endpointsEnabled, PrivateApiAccess(allowlist, isTrial), userId.isDefined, authorised))
      case _                                    => Some(ApiAvailability(version.endpointsEnabled, PublicApiAccess(), userId.isDefined, authorised = true))
    }
  }
}
