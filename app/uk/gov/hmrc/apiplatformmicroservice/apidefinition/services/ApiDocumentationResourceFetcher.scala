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
import scala.concurrent.{ExecutionContext, Future}

import cats.data.OptionT
import cats.implicits._
import org.apache.pekko.stream.Materializer

import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApiVersionNbr
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.{ExtendedApiDefinition, ExtendedApiVersion}
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models._
import uk.gov.hmrc.apiplatformmicroservice.common.{ApplicationLogger, StreamedResponseResourceHelper}

@Singleton
class ApiDocumentationResourceFetcher @Inject() (
    principalDefinitionService: PrincipalApiDefinitionService,
    subordinateDefinitionService: SubordinateApiDefinitionService,
    extendedApiDefinitionFetcher: ExtendedApiDefinitionForCollaboratorFetcher
  )(implicit override val ec: ExecutionContext,
    override val mat: Materializer
  ) extends StreamedResponseResourceHelper
    with ApplicationLogger {

  sealed trait WhereToLook
  case object Both           extends WhereToLook
  case object ProductionOnly extends WhereToLook

  def fetch(resourceId: ResourceId)(implicit hc: HeaderCarrier): Future[Option[HttpResponse]] = {
    (
      for {
        apiDefinition <- OptionT(extendedApiDefinitionFetcher.fetchCached(resourceId.serviceName, None))
        whereToLook   <- OptionT.fromOption[Future](findWhereToLook(apiDefinition, resourceId))
        response      <- fetchResource(whereToLook, resourceId)
      } yield response
    ).value
  }

  private def findWhereToLook(apiDefinition: ExtendedApiDefinition, resourceId: ResourceId): Option[WhereToLook] = {
    lazy val version                                 = resourceId.versionNbr
    lazy val findVersion: Option[ExtendedApiVersion] = apiDefinition.versions.find(_.version == version)

    val whereToLookForVersion: (ExtendedApiVersion) => WhereToLook = (eav) => {
      logger.info(s"Availability of $resourceId - Sandbox: ${eav.sandboxAvailability.isDefined} Production: ${eav.productionAvailability.isDefined}")
      if (eav.sandboxAvailability.isDefined) Both else ProductionOnly
    }

    version match {
      case ApiVersionNbr("common") => Both.some
      case _                       => findVersion.map(whereToLookForVersion)
    }
  }

  private def fetchResource(whereToLook: WhereToLook, resourceId: ResourceId)(implicit hc: HeaderCarrier): OptionT[Future, HttpResponse] =
    whereToLook match {
      case Both           => fetchSubordinateOrPrincipal(resourceId)
      case ProductionOnly => fetchPrincipalResourceOnly(resourceId)
    }

  private def logAndHandleErrorsAsNone(connectorName: String)(resourceId: ResourceId)(x: HttpResponse): OptionT[Future, HttpResponse] = {
    logger.info(s"$connectorName response code: ${x.status} for ${resourceId}")

    if (x.status >= 200 && x.status <= 299) {
      OptionT.some(x)
    } else {
      OptionT.none
    }
  }

  private def fetchSubordinateOrPrincipal(resourceId: ResourceId)(implicit hc: HeaderCarrier): OptionT[Future, HttpResponse] = {

    val subordinateData: OptionT[Future, HttpResponse] =
      OptionT(subordinateDefinitionService.fetchApiDocumentationResource(resourceId))
        .flatMap(logAndHandleErrorsAsNone("Subordinate")(resourceId))

    lazy val principalData: OptionT[Future, HttpResponse] =
      OptionT(principalDefinitionService.fetchApiDocumentationResource(resourceId))
        .flatMap(logAndHandleErrorsAsNone("Principal")(resourceId))

    subordinateData
      .orElse(principalData)
  }

  private def fetchPrincipalResourceOnly(resourceId: ResourceId)(implicit hc: HeaderCarrier): OptionT[Future, HttpResponse] = {
    OptionT(principalDefinitionService.fetchApiDocumentationResource(resourceId))
      .flatMap(logAndHandleErrorsAsNone("Principal")(resourceId))
  }
}
