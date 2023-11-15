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

import scala.concurrent.{ExecutionContext, Future}

import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}

import play.api.libs.json.JsValue
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApiVersionNbr
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.{ApiDefinition, ServiceName}
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.connectors.ApiDefinitionConnector
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ResourceId
import uk.gov.hmrc.apiplatformmicroservice.common.{EnvironmentAware, LogWrapper}
import uk.gov.hmrc.apiplatformmicroservice.metrics.RecordMetrics

abstract class ApiDefinitionService extends LogWrapper with RecordMetrics {
  def connector: ApiDefinitionConnector
  def enabled: Boolean

  def fetchDefinition(serviceName: ServiceName)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[ApiDefinition]] = {
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

  def fetchAllApiDefinitions(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[List[ApiDefinition]] = {
    lazy val failFn = (e: Throwable) => s"FetchAllApiDefinitions failed $e"

    if (enabled) {
      record {
        log(failFn) {
          connector.fetchAllApiDefinitions
        }
      }
    } else {
      Future.successful(List.empty)
    }
  }

  def fetchAllNonOpenAccessApiDefinitions(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[List[ApiDefinition]] = {
    for {
      allApis <- fetchAllApiDefinitions
      open     = allApis.filterNot(_.isOpenAccess)
    } yield open
  }

  def fetchAllOpenAccessApiDefinitions(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[List[ApiDefinition]] = {
    for {
      allApis <- fetchAllApiDefinitions
      open     = allApis.filter(_.isOpenAccess)
    } yield open
  }

  def fetchApiDocumentationResource(resourceId: ResourceId)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[WSResponse]] = {
    import resourceId._
    lazy val failFn = (e: Throwable) => s"fetchApiDocumentationResource($serviceName, $versionNbr, $resource) failed $e"

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

  def fetchApiSpecification(serviceName: ServiceName, version: ApiVersionNbr)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[JsValue]] = {
    lazy val failFn = (e: Throwable) => s"fetchApiSpecification($serviceName, $version) failed $e"

    if (enabled) {
      record {
        log(failFn) {
          connector.fetchApiSpecification(serviceName, version)
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
    @Named("principal") val principal: ApiDefinitionService
  ) extends EnvironmentAware[ApiDefinitionService]
