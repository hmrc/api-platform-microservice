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

package uk.gov.hmrc.apiplatformmicroservice.apidefinition.models

import cats.data.{NonEmptyList => NEL}

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
 
case class ApiDefinition(
    serviceName: String,
    serviceBaseUrl: String,
    name: String,
    description: String,
    context: ApiContext,
    requiresTrust: Boolean = false,
    isTestSupport: Boolean = false,
    versions: List[ApiVersionDefinition],
    categories: List[ApiCategory] = List.empty
  )

case class ApiVersionDefinition(
    version: ApiVersionNbr,
    status: ApiStatus,
    access: ApiAccess,
    endpoints: List[Endpoint],
    endpointsEnabled: Boolean = false,
    versionSource: ApiVersionSource = ApiVersionSource.UNKNOWN
  )

// case class Endpoint(endpointName: String, uriPattern: String, method: HttpMethod, authType: AuthType, queryParameters: List[QueryParameter] = List.empty)
