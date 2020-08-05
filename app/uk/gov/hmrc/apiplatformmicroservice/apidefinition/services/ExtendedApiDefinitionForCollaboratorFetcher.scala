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
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.ApplicationIdsForCollaboratorFetcher
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ExtendedApiDefinitionForCollaboratorFetcher @Inject() (
    principalDefinitionService: PrincipalApiDefinitionService,
    subordinateDefinitionService: SubordinateApiDefinitionService,
    appIdsFetcher: ApplicationIdsForCollaboratorFetcher
  )(implicit ec: ExecutionContext) {

  def fetch(serviceName: String, email: Option[String])(implicit hc: HeaderCarrier): Future[Option[ExtendedAPIDefinition]] = {
    for {
      principalDefinition <- principalDefinitionService.fetchDefinition(serviceName)
      subordinateDefinition <- subordinateDefinitionService.fetchDefinition(serviceName)
      applicationIds <- email.fold(successful(Seq.empty[String]))(appIdsFetcher.fetch(_))
    } yield createExtendedApiDefinition(principalDefinition, subordinateDefinition, applicationIds, email)
  }

  private def createExtendedApiDefinition(
      maybePrincipalDefinition: Option[APIDefinition],
      maybeSubordinateDefinition: Option[APIDefinition],
      applicationIds: Seq[String],
      email: Option[String]
    ): Option[ExtendedAPIDefinition] = {

    def toCombinedAPIDefinition(apiDefinition: APIDefinition, principalVersions: Seq[APIVersion], subordinateVersions: Seq[APIVersion]): Option[ExtendedAPIDefinition] = {
      if (apiDefinition.requiresTrust) {
        None
      } else {
        val extendedVersions = createExtendedApiVersions(principalVersions, subordinateVersions, applicationIds, email)
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
      principalVersions: Seq[APIVersion],
      subordinateVersions: Seq[APIVersion],
      applicationIds: Seq[String],
      email: Option[String]
    ): Seq[ExtendedAPIVersion] = {
    val allVersions = (principalVersions.map(_.version) ++ subordinateVersions.map(_.version)).distinct.sorted
    allVersions map { version =>
      combineVersion(principalVersions.find(_.version == version), subordinateVersions.find(_.version == version), applicationIds, email)
    } filter { version =>
      version.status != RETIRED
    }
  }

  private def combineVersion(
      maybePrincipalVersion: Option[APIVersion],
      maybeSubordinateVersion: Option[APIVersion],
      applicationIds: Seq[String],
      email: Option[String]
    ): ExtendedAPIVersion = {

    (maybePrincipalVersion, maybeSubordinateVersion) match {
      case (Some(principalVersion), None)                     =>
        toExtendedApiVersion(principalVersion, availability(principalVersion, applicationIds, email), None)
      case (None, Some(subordinateVersion))                   =>
        toExtendedApiVersion(subordinateVersion, None, availability(subordinateVersion, applicationIds, email))
      case (Some(principalVersion), Some(subordinateVersion)) =>
        toExtendedApiVersion(subordinateVersion, availability(principalVersion, applicationIds, email), availability(subordinateVersion, applicationIds, email))
      case (None, None)                                       =>
        throw new IllegalStateException("It's impossible to get here from the call site")
    }
  }

  private def toExtendedApiVersion(apiVersion: APIVersion, productionAvailability: Option[APIAvailability], sandboxAvailability: Option[APIAvailability]): ExtendedAPIVersion = {
    ExtendedAPIVersion(
      version = apiVersion.version,
      status = apiVersion.status,
      endpoints = apiVersion.endpoints,
      productionAvailability = productionAvailability,
      sandboxAvailability = sandboxAvailability
    )
  }

  private def availability(version: APIVersion, applicationIds: Seq[String], email: Option[String]): Option[APIAvailability] = {
    version.access match {
      case PrivateApiAccess(whitelist, isTrial) =>
        Some(APIAvailability(version.endpointsEnabled, PrivateApiAccess(whitelist, isTrial), email.isDefined, authorised = applicationIds.intersect(whitelist).nonEmpty))

      case _                                    => Some(APIAvailability(version.endpointsEnabled, PublicApiAccess(), email.isDefined, authorised = true))
    }
  }
}
