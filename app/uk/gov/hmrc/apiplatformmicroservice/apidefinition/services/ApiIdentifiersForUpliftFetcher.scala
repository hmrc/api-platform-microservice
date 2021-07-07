/*
 * Copyright 2021 HM Revenue & Customs
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
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models._
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiStatus.RETIRED
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApiIdentifiersForUpliftFetcher @Inject() (
    apiDefinitionService: EnvironmentAwareApiDefinitionService
  )(implicit ec: ExecutionContext) {

  private val EXAMPLE = ApiCategory("EXAMPLE")

  def fetch(implicit hc: HeaderCarrier): Future[List[ApiIdentifier]] = {
    for {
      defs <- apiDefinitionService.principal.fetchAllApiDefinitions
      filteredDefs = defs.filterNot(d => d.isTestSupport || d.categories.contains(EXAMPLE))
      ids = filteredDefs.flatMap(d => d.versions.filterNot(_.status == RETIRED).map(v => ApiIdentifier(d.context, v.version)))
    } yield ids
      
  }
}
