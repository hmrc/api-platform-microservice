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

@Singleton
class ExtendedApiDefinitionForCollaboratorFetcher @Inject() (
    principalDefinitionService: PrincipalApiDefinitionService,
    subordinateDefinitionService: SubordinateApiDefinitionService,
    appIdsFetcher: ApplicationIdsForCollaboratorFetcher,
    subscriptionsForCollaboratorFetcher: SubscriptionsForCollaboratorFetcher
  )(implicit ec: ExecutionContext) {

  def fetch(serviceName: String, email: Option[String])(implicit hc: HeaderCarrier): Future[Option[ExtendedAPIDefinition]] = {
    for {
      principalDefinition <- principalDefinitionService.fetchDefinition(serviceName)
      subordinateDefinition <- subordinateDefinitionService.fetchDefinition(serviceName)
      applicationIds <- email.fold(successful(Set.empty[ApplicationId]))(appIdsFetcher.fetch(_))
      subscriptions <- email.fold(successful(Set.empty[ApiIdentifier]))(subscriptionsForCollaboratorFetcher.fetch(_))
    } yield createExtendedApiDefinition(principalDefinition, subordinateDefinition, applicationIds, subscriptions, email)
  }

  private def createExtendedApiDefinition(
      maybePrincipalDefinition: Option[APIDefinition],
      maybeSubordinateDefinition: Option[APIDefinition],
      applicationIds: Set[ApplicationId],
      subscriptions: Set[ApiIdentifier],
      email: Option[String]
    ): Option[ExtendedAPIDefinition] = {

    def toCombinedAPIDefinition(
        apiDefinition: APIDefinition,
        principalVersions: Seq[ApiVersionDefinition],
        subordinateVersions: Seq[ApiVersionDefinition]
      ): Option[ExtendedAPIDefinition] = {
      if (apiDefinition.requiresTrust) {
        None
      } else {
        val extendedVersions = createExtendedApiVersions(apiDefinition.context, principalVersions, subordinateVersions, applicationIds, subscriptions, email)
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
            extendedVersions
          ))
        }
      }
    }

    (maybePrincipalDefinition, maybeSubordinateDefinition) match {
      case (Some(principalDefinition), None)                        =>
        toCombinedAPIDefinition(principalDefinition, principalDefinition.versions, Seq.empty)
      case (None, Some(subordinateDefinition))                      =>
        toCombinedAPIDefinition(subordinateDefinition, Seq.empty, subordinateDefinition.versions)
      case (Some(principalDefinition), Some(subordinateDefinition)) =>
        toCombinedAPIDefinition(subordinateDefinition, principalDefinition.versions, subordinateDefinition.versions)
      case _                                                        => None
    }
  }

  private def createExtendedApiVersions(
      context: ApiContext, 
      principalVersions: Seq[ApiVersionDefinition],
      subordinateVersions: Seq[ApiVersionDefinition],
      applicationIds: Set[ApplicationId],
      subscriptions: Set[ApiIdentifier],
      email: Option[String]
    ): Seq[ExtendedAPIVersion] = {
    val allVersions = (principalVersions.map(_.version) ++ subordinateVersions.map(_.version)).distinct.sorted
    allVersions map { version =>
      combineVersion(context, principalVersions.find(_.version == version), subordinateVersions.find(_.version == version), applicationIds, subscriptions, email)
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
      email: Option[String]
    ): ExtendedAPIVersion = {

    (maybePrincipalVersion, maybeSubordinateVersion) match {
      case (Some(principalVersion), None)                     =>
        toExtendedApiVersion(principalVersion, availability(context, principalVersion, applicationIds, subscriptions, email), None)
      case (None, Some(subordinateVersion))                   =>
        toExtendedApiVersion(subordinateVersion, None, availability(context, subordinateVersion, applicationIds, subscriptions, email))
      case (Some(principalVersion), Some(subordinateVersion)) =>
        toExtendedApiVersion(subordinateVersion, availability(context, principalVersion, applicationIds, subscriptions, email), availability(context, subordinateVersion, applicationIds, subscriptions, email))
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

  private def availability(context: ApiContext, version: ApiVersionDefinition, applicationIds: Set[ApplicationId], subscriptions: Set[ApiIdentifier], email: Option[String]): Option[APIAvailability] = {
    version.access match {
      case PrivateApiAccess(whitelist, isTrial) =>
        val authorised = applicationIds.intersect(whitelist.toSet).nonEmpty || subscriptions.contains(ApiIdentifier(context, version.version))
        Some(APIAvailability(version.endpointsEnabled, PrivateApiAccess(whitelist, isTrial), email.isDefined, authorised))
      case _                                    => Some(APIAvailability(version.endpointsEnabled, PublicApiAccess(), email.isDefined, authorised = true))
    }
  }
}
