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
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models._
import uk.gov.hmrc.apiplatformmicroservice.common.StreamedResponseResourceHelper
import uk.gov.hmrc.http.HeaderCarrier
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.apiplatformmicroservice.common.ApplicationLogger
import play.api.libs.json.JsValue

@Singleton
class ApiSpecificationFetcher @Inject() (
    apiDefinitionService: EnvironmentAwareApiDefinitionService,
    extendedApiDefinitionFetcher: ExtendedApiDefinitionForCollaboratorFetcher
  )(implicit override val ec: ExecutionContext,
    override val mat: Materializer
  ) extends StreamedResponseResourceHelper
    with ApplicationLogger {

  def fetch(serviceName: String, version: ApiVersion)(implicit hc: HeaderCarrier): Future[Option[JsValue]] = {
    (
      for {
        apiVersion <- fetchApiVersion(serviceName, version)
        response   <- fetchApiSpecification(apiVersion.sandboxAvailability.isDefined, serviceName, version)
      } yield response
    )
      .value
  }

  private def fetchApiVersion(serviceName: String, version: ApiVersion)(implicit hc: HeaderCarrier): OptionT[Future, ExtendedApiVersion] = {
    OptionT(extendedApiDefinitionFetcher.fetch(serviceName, None))
      .mapFilter(defn => defn.versions.find(_.version == version))
  }

  private def fetchApiSpecification(isAvailableInSandbox: Boolean, serviceName: String, version: ApiVersion)(implicit hc: HeaderCarrier): OptionT[Future, JsValue] = {
    if (isAvailableInSandbox) {
      fetchSubordinateOrPrincipal(serviceName, version)
    } else {
      fetchPrincipalApiSpecification(serviceName, version)
    }
  }

  private def fetchSubordinateOrPrincipal(serviceName: String, version: ApiVersion)(implicit hc: HeaderCarrier): OptionT[Future, JsValue] = {
    fetchSubordinateApiSpecification(serviceName, version)
      .orElse(fetchPrincipalApiSpecification(serviceName, version))
  }

  private def fetchSubordinateApiSpecification(serviceName: String, version: ApiVersion)(implicit hc: HeaderCarrier): OptionT[Future, JsValue] = {
    OptionT(apiDefinitionService.subordinate.fetchApiSpecification(serviceName, version))
  }

  private def fetchPrincipalApiSpecification(serviceName: String, version: ApiVersion)(implicit hc: HeaderCarrier): OptionT[Future, JsValue] = {
    OptionT(apiDefinitionService.principal.fetchApiSpecification(serviceName, version))
  }
}
