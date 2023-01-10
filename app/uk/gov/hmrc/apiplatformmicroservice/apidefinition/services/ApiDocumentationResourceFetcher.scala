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

import akka.stream.Materializer
import cats.data.OptionT
import cats.implicits._
import javax.inject.{Inject, Singleton}
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models._
import uk.gov.hmrc.apiplatformmicroservice.common.StreamedResponseResourceHelper
import uk.gov.hmrc.http.HeaderCarrier
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.apiplatformmicroservice.common.ApplicationLogger

@Singleton
class ApiDocumentationResourceFetcher @Inject() (
    principalDefinitionService: PrincipalApiDefinitionService,
    subordinateDefinitionService: SubordinateApiDefinitionService,
    extendedApiDefinitionFetcher: ExtendedApiDefinitionForCollaboratorFetcher
  )(implicit override val ec: ExecutionContext,
    override val mat: Materializer
  ) extends StreamedResponseResourceHelper
    with ApplicationLogger {

  trait WhereToLook
  case object Both           extends WhereToLook
  case object ProductionOnly extends WhereToLook

  def fetch(resourceId: ResourceId)(implicit hc: HeaderCarrier): Future[Option[WSResponse]] = {
    (
      for {
        apiDefinition <- OptionT(extendedApiDefinitionFetcher.fetch(resourceId.serviceName, None))
        whereToLook   <- OptionT.fromOption[Future](findWhereToLook(apiDefinition, resourceId))
        response      <- fetchResource(whereToLook, resourceId)
      } yield response
    ).value
  }

  private def findWhereToLook(apiDefinition: ExtendedApiDefinition, resourceId: ResourceId): Option[WhereToLook] = {
    lazy val version                                 = resourceId.version
    lazy val findVersion: Option[ExtendedApiVersion] = apiDefinition.versions.find(_.version == version)

    val whereToLookForVersion: (ExtendedApiVersion) => WhereToLook = (eav) => {
      logger.info(s"Availability of $resourceId - Sandbox: ${eav.sandboxAvailability.isDefined} Production: ${eav.productionAvailability.isDefined}")
      if (eav.sandboxAvailability.isDefined) Both else ProductionOnly
    }

    version match {
      case ApiVersion("common") => Both.some
      case _                    => findVersion.map(whereToLookForVersion)
    }
  }

  private def fetchResource(whereToLook: WhereToLook, resourceId: ResourceId)(implicit hc: HeaderCarrier): OptionT[Future, WSResponse] =
    whereToLook match {
      case Both           => fetchSubordinateOrPrincipal(resourceId)
      case ProductionOnly => fetchPrincipalResourceOnly(resourceId)
    }

  private def logAndHandleErrorsAsNone(connectorName: String)(resourceId: ResourceId)(x: WSResponse): OptionT[Future, WSResponse] = {
    logger.info(s"$connectorName response code: ${x.status} for ${resourceId}")

    if (x.status >= 200 && x.status <= 299) {
      OptionT.some(x)
    } else {
      OptionT.none
    }
  }

  private def fetchSubordinateOrPrincipal(resourceId: ResourceId)(implicit hc: HeaderCarrier): OptionT[Future, WSResponse] = {

    val subordinateData: OptionT[Future, WSResponse] =
      OptionT(subordinateDefinitionService.fetchApiDocumentationResource(resourceId))
        .flatMap(logAndHandleErrorsAsNone("Subordinate")(resourceId))

    lazy val principalData: OptionT[Future, WSResponse] =
      OptionT(principalDefinitionService.fetchApiDocumentationResource(resourceId))
        .flatMap(logAndHandleErrorsAsNone("Principal")(resourceId))

    subordinateData
      .orElse(principalData)
  }

  private def fetchPrincipalResourceOnly(resourceId: ResourceId)(implicit hc: HeaderCarrier): OptionT[Future, WSResponse] = {
    OptionT(principalDefinitionService.fetchApiDocumentationResource(resourceId))
      .flatMap(logAndHandleErrorsAsNone("Principal")(resourceId))
  }
}
