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

import akka.stream.Materializer
import cats.data.OptionT
import cats.implicits._
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models._
import uk.gov.hmrc.apiplatformmicroservice.common.StreamedResponseResourceHelper
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApiDocumentationResourceFetcher @Inject()(principalDefinitionService: PrincipalApiDefinitionService,
                                                subordinateDefinitionService: SubordinateApiDefinitionService,
                                                extendedApiDefinitionFetcher: ExtendedApiDefinitionForCollaboratorFetcher)
                                               (implicit override val ec: ExecutionContext, override val mat: Materializer)
  extends StreamedResponseResourceHelper {

  def fetch(resourceId: ResourceId)(implicit hc: HeaderCarrier): Future[Option[WSResponse]] = {
    for {
      apiVersion <- fetchApiVersion(resourceId)
      _ = Logger.info(
        s"Availability of $resourceId - Sandbox: ${apiVersion.sandboxAvailability.isDefined} Production: ${apiVersion.productionAvailability.isDefined}")
      response <- fetchResource(apiVersion.sandboxAvailability.isDefined, resourceId)
    } yield response.some
  }

  private def fetchApiVersion(resourceId: ResourceId)(implicit hc: HeaderCarrier): Future[ExtendedAPIVersion] = {
    def findVersion(definition: ExtendedAPIDefinition): Option[ExtendedAPIVersion] = {
      definition.versions.find(_.version == resourceId.version)
    }

    val error = Future.failed[ExtendedAPIVersion](
      new IllegalArgumentException(
        s"Version ${resourceId.version} of ${resourceId.serviceName} not found"))

    OptionT(extendedApiDefinitionFetcher.fetch(resourceId.serviceName, None))
      .mapFilter(findVersion)
      .getOrElseF(error)
  }

  private def fetchResource(isAvailableInSandbox: Boolean, resourceId: ResourceId)(implicit hc: HeaderCarrier): Future[WSResponse] = {
    if (isAvailableInSandbox) {
      fetchSubordinateOrPrincipal(resourceId)
    } else {
      fetchPrincipalResourceOnly(resourceId)
    }
  }

  private def fetchSubordinateOrPrincipal(resourceId: ResourceId)(implicit hc: HeaderCarrier) = {
    val subordinateData: OptionT[Future, WSResponse] =
      OptionT(subordinateDefinitionService.fetchApiDocumentationResource(resourceId))
        .flatMap(mapStatusCodeToOption("Subordinate"))

    lazy val principalData: OptionT[Future, WSResponse] =
      OptionT(principalDefinitionService.fetchApiDocumentationResource(resourceId))
        .flatMap(mapStatusCodeToOption("Principal"))

    subordinateData
      .orElse(principalData)
      .getOrElseF(failedDueToNotFoundException(resourceId))
  }

  private def mapStatusCodeToOption(connectorName: String)(
    x: WSResponse): OptionT[Future, WSResponse] = {
    Logger.info(s"$connectorName response code: ${x.status}")
    if (x.status >= 200 && x.status <= 299) {
      OptionT.some(x)
    } else {
      OptionT.none
    }
  }

  private def fetchPrincipalResourceOnly(resourceId: ResourceId)(implicit hc: HeaderCarrier) = {
    OptionT(principalDefinitionService.fetchApiDocumentationResource(resourceId))
      .getOrElseF(failedDueToNotFoundException(resourceId))
  }
}
