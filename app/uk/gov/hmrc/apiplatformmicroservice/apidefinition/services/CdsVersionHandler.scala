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

import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models._

/** CDS have a number of API versions that exist, and can be subscribed to, in the sandbox environment.
  *
  * When uplifting a sandbox application to production, we need to map API versions for these contexts to v1.0.
  */
object CdsVersionHandler {
  private val apiVersionOne = ApiVersion("1.0")
  private val apiVersionTwo = ApiVersion("2.0")

  val specialCaseContexts: Set[ApiContext] = Set(
    "customs/declarations",
    "customs/declarations-information",
    "customs/inventory-linking/exports",
    "customs/inventory-linking-imports"
  ).map(ApiContext(_))

  val populateSpecialCases: (Set[ApiIdentifier]) => Set[ApiIdentifier] =
    (in) =>
      in.flatMap(id =>
        if (specialCaseContexts.contains(id.context) && id.version == apiVersionOne) {
          Set(id, id.copy(version = apiVersionTwo))
        } else {
          Set(id)
        }
      )

  val adjustSpecialCaseVersions: (Set[ApiIdentifier]) => Set[ApiIdentifier] =
    (in) =>
      in.map(id =>
        if (specialCaseContexts.contains(id.context) && id.version == apiVersionTwo) {
          id.copy(version = apiVersionOne)
        } else {
          id
        }
      )
}
