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

package uk.gov.hmrc.apiplatformmicroservice.apidefinition.models

import cats.data.{NonEmptyList => NEL}

case class ExtendedAPIDefinition(
    serviceName: String,
    name: String,
    description: String,
    context: String,
    requiresTrust: Boolean,
    isTestSupport: Boolean,
    versions: Seq[ExtendedAPIVersion])

case class ExtendedAPIVersion(
    version: String,
    status: APIStatus,
    endpoints: NEL[Endpoint],
    productionAvailability: Option[APIAvailability],
    sandboxAvailability: Option[APIAvailability])

case class APIAvailability(
    endpointsEnabled: Boolean,
    access: APIAccess,
    loggedIn: Boolean,
    authorised: Boolean)
