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

import play.api.libs.ws.WSResponse
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.connectors.ApiDefinitionConnector
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.{APIDefinition, ResourceId}
import uk.gov.hmrc.apiplatformmicroservice.common.LogWrapper
import uk.gov.hmrc.apiplatformmicroservice.metrics.RecordMetrics
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.apiplatformmicroservice.common.EnvironmentAware
import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}

abstract class ApiDefinitionService extends LogWrapper with RecordMetrics {
  def connector: ApiDefinitionConnector
  def enabled: Boolean

  def fetchDefinition(serviceName: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[APIDefinition]] = {
    lazy val failFn = (e: Throwable) => s"fetchDefinition($serviceName) failed $e"

    if (enabled) {
      record {
        log(failFn) {
          connector.fetchApiDefinition(serviceName)
        }
      }
    } else {
      Future.successful(None)
    }
  }

  def fetchAllDefinitions(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[APIDefinition]] = {
    lazy val failFn = (e: Throwable) => s"fetchAllDefinitions failed $e"

    if (enabled) {
      record {
        log(failFn) {
          connector.fetchAllApiDefinitions
        }
      }
    } else {
      Future.successful(Seq.empty)
    }
  }

  def fetchApiDocumentationResource(resourceId: ResourceId)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[WSResponse]] = {
    import resourceId._
    lazy val failFn = (e: Throwable) => s"fetchApiDocumentationResource($serviceName, ${version.value}, $resource) failed $e"

    if (enabled) {
      record {
        log(failFn) {
          connector.fetchApiDocumentationResource(resourceId)
        }
      }
    } else {
      Future.successful(None)
    }
  }
}

@Singleton
class EnvironmentAwareApiDefinitionService @Inject() (
    @Named("subordinate") val subordinate: ApiDefinitionService,
    @Named("principal") val principal: ApiDefinitionService)
    extends EnvironmentAware[ApiDefinitionService]
