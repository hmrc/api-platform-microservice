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
class ExtendedApiDefinitionForCollaboratorFetcher @Inject()(principalDefinitionService: PrincipalApiDefinitionService,
                                                            subordinateDefinitionService: SubordinateApiDefinitionService,
                                                            appIdsFetcher: ApplicationIdsForCollaboratorFetcher)
                                                           (implicit ec: ExecutionContext) {

  def apply(serviceName: String, email: Option[String])(implicit hc: HeaderCarrier): Future[Option[ExtendedAPIDefinition]] = {
    for {
      principalDefinition <- principalDefinitionService.fetchDefinition(serviceName)
      subordinateDefinition <- subordinateDefinitionService.fetchDefinition(serviceName)
      applicationIds <- email.fold(successful(Seq.empty[String]))(appIdsFetcher(_))
    } yield createExtendedApiDefinition(principalDefinition, subordinateDefinition, applicationIds, email)
  }

  private def createExtendedApiDefinition(maybePrincipalDefinition: Option[APIDefinition], maybeSubordinateDefinition: Option[APIDefinition],
                                          applicationIds: Seq[String], email: Option[String]): Option[ExtendedAPIDefinition] = {

    def toCombinedAPIDefinition(apiDefinition: APIDefinition,
                                principalVersions: Seq[APIVersion], subordinateVersions: Seq[APIVersion]): Option[ExtendedAPIDefinition] = {
      if (apiDefinition.requiresTrust) {
        None
      } else {
        Some(ExtendedAPIDefinition(
          apiDefinition.serviceName,
          apiDefinition.name,
          apiDefinition.description,
          apiDefinition.context,
          apiDefinition.requiresTrust,
          apiDefinition.isTestSupport,
          createExtendedApiVersions(principalVersions, subordinateVersions, applicationIds, email)))
      }
    }

    (maybePrincipalDefinition, maybeSubordinateDefinition) match {
      case (Some(principalDefinition), None) =>
        toCombinedAPIDefinition(principalDefinition, principalDefinition.versions, Seq.empty)
      case (None, Some(subordinateDefinition)) =>
        toCombinedAPIDefinition(subordinateDefinition, Seq.empty, subordinateDefinition.versions)
      case (Some(principalDefinition), Some(subordinateDefinition)) =>
        toCombinedAPIDefinition(subordinateDefinition, principalDefinition.versions, subordinateDefinition.versions)
      case _ => None
    }
  }

  private def createExtendedApiVersions(principalVersions: Seq[APIVersion], subordinateVersions: Seq[APIVersion],
                                        applicationIds: Seq[String], email: Option[String]): Seq[ExtendedAPIVersion] = {
    val combinedVersions = (subordinateVersions ++ principalVersions
      .filterNot(pv => subordinateVersions.exists(sv => sv.version == pv.version)))
      .filter(_.status != RETIRED)
      .sortBy(_.version)
    toExtendedApiVersion(combinedVersions, applicationIds, email)
  }

  private def toExtendedApiVersion(apiVersions: Seq[APIVersion], applicationIds: Seq[String], email: Option[String]): Seq[ExtendedAPIVersion] = {
    apiVersions map { version =>
      ExtendedAPIVersion(
        version = version.version,
        status = version.status,
        endpoints = version.endpoints,
        productionAvailability = availability(version, applicationIds, email),
        sandboxAvailability = availability(version, applicationIds, email))
    }
  }

  private def availability(version: APIVersion, applicationIds: Seq[String], email: Option[String]): Option[APIAvailability] = {
    version.access match {
      case PrivateApiAccess(whitelist, isTrial) => Some(APIAvailability(version.endpointsEnabled,
        PrivateApiAccess(whitelist, isTrial),
        email.isDefined,
        authorised = applicationIds.intersect(whitelist).nonEmpty))

      case _ => Some(APIAvailability(version.endpointsEnabled,
        PublicApiAccess(),
        email.isDefined,
        authorised = true))
    }
  }
}
