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

package uk.gov.hmrc.apiplatformmicroservice.apidefinition.services

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.APIStatus.RETIRED
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models._
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.ApplicationIdsForCollaboratorFetcher
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.SubscriptionsForCollaboratorFetcher
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.DeveloperIdentifier

@Singleton
class ExtendedApiDefinitionForCollaboratorFetcher @Inject() (
    principalDefinitionService: PrincipalApiDefinitionService,
    subordinateDefinitionService: SubordinateApiDefinitionService,
    appIdsFetcher: ApplicationIdsForCollaboratorFetcher,
    subscriptionsForCollaboratorFetcher: SubscriptionsForCollaboratorFetcher
  )(implicit ec: ExecutionContext) {

  def fetch(serviceName: String, developerId: Option[DeveloperIdentifier])(implicit hc: HeaderCarrier): Future[Option[ExtendedAPIDefinition]] = {
    for {
      principalDefinition <- principalDefinitionService.fetchDefinition(serviceName)
      subordinateDefinition <- subordinateDefinitionService.fetchDefinition(serviceName)
      applicationIds <- developerId.fold(successful(Set.empty[ApplicationId]))(appIdsFetcher.fetch)
      subscriptions <- developerId.fold(successful(Set.empty[ApiIdentifier]))(subscriptionsForCollaboratorFetcher.fetch)
    } yield createExtendedApiDefinition(principalDefinition, subordinateDefinition, applicationIds, subscriptions, developerId)
  }

  private def createExtendedApiDefinition(
      maybePrincipalDefinition: Option[APIDefinition],
      maybeSubordinateDefinition: Option[APIDefinition],
      applicationIds: Set[ApplicationId],
      subscriptions: Set[ApiIdentifier],
      developerId: Option[DeveloperIdentifier]
    ): Option[ExtendedAPIDefinition] = {

    def toCombinedAPIDefinition(
        apiDefinition: APIDefinition,
        principalVersions: List[ApiVersionDefinition],
        subordinateVersions: List[ApiVersionDefinition]
      ): Option[ExtendedAPIDefinition] = {
      if (apiDefinition.requiresTrust) {
        None
      } else {
        val extendedVersions = createExtendedApiVersions(apiDefinition.context, principalVersions, subordinateVersions, applicationIds, subscriptions, developerId)
        if (extendedVersions.isEmpty) {
          None
        } else {
          Some(ExtendedAPIDefinition(
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
        toCombinedAPIDefinition(principalDefinition, principalDefinition.versions, List.empty)
      case (None, Some(subordinateDefinition))                      =>
        toCombinedAPIDefinition(subordinateDefinition, List.empty, subordinateDefinition.versions)
      case (Some(principalDefinition), Some(subordinateDefinition)) =>
        toCombinedAPIDefinition(subordinateDefinition, principalDefinition.versions, subordinateDefinition.versions)
      case _                                                        => None
    }
  }

  private def createExtendedApiVersions(
      context: ApiContext, 
      principalVersions: List[ApiVersionDefinition],
      subordinateVersions: List[ApiVersionDefinition],
      applicationIds: Set[ApplicationId],
      subscriptions: Set[ApiIdentifier],
      developerId: Option[DeveloperIdentifier]
    ): List[ExtendedAPIVersion] = {
    val allVersions = (principalVersions.map(_.version) ++ subordinateVersions.map(_.version)).distinct.sorted
    allVersions map { version =>
      combineVersion(context, principalVersions.find(_.version == version), subordinateVersions.find(_.version == version), applicationIds, subscriptions, developerId)
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
      developerId: Option[DeveloperIdentifier]
    ): ExtendedAPIVersion = {

    (maybePrincipalVersion, maybeSubordinateVersion) match {
      case (Some(principalVersion), None)                     =>
        toExtendedApiVersion(principalVersion, availability(context, principalVersion, applicationIds, subscriptions, developerId), None)
      case (None, Some(subordinateVersion))                   =>
        toExtendedApiVersion(subordinateVersion, None, availability(context, subordinateVersion, applicationIds, subscriptions, developerId))
      case (Some(principalVersion), Some(subordinateVersion)) =>
        toExtendedApiVersion(subordinateVersion, availability(context, principalVersion, applicationIds, subscriptions, developerId), availability(context, subordinateVersion, applicationIds, subscriptions, developerId))
      case (None, None)                                       =>
        throw new IllegalStateException("It's impossible to get here from the call site")
    }
  }

  private def toExtendedApiVersion(
      apiVersion: ApiVersionDefinition,
      productionAvailability: Option[APIAvailability],
      sandboxAvailability: Option[APIAvailability]
    ): ExtendedAPIVersion = {
    ExtendedAPIVersion(
      version = apiVersion.version,
      status = apiVersion.status,
      endpoints = apiVersion.endpoints,
      productionAvailability = productionAvailability,
      sandboxAvailability = sandboxAvailability
    )
  }

  private def availability(context: ApiContext, version: ApiVersionDefinition, applicationIds: Set[ApplicationId], subscriptions: Set[ApiIdentifier], developerId: Option[DeveloperIdentifier]): Option[APIAvailability] = {
    version.access match {
      case PrivateApiAccess(whitelist, isTrial) =>
        val authorised = applicationIds.intersect(whitelist.toSet).nonEmpty || subscriptions.contains(ApiIdentifier(context, version.version))
        Some(APIAvailability(version.endpointsEnabled, PrivateApiAccess(whitelist, isTrial), developerId.isDefined, authorised))
      case _                                    => Some(APIAvailability(version.endpointsEnabled, PublicApiAccess(), developerId.isDefined, authorised = true))
    }
  }
}
