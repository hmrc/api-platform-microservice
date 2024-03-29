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

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.apis.domain.models._

@Singleton
class ApiIdentifiersForUpliftFetcher @Inject() (
    apiDefinitionService: EnvironmentAwareApiDefinitionService
  )(implicit ec: ExecutionContext
  ) {

  def fetch(implicit hc: HeaderCarrier): Future[Set[ApiIdentifier]] = {
    for {
      defs                <- apiDefinitionService.principal.fetchAllApiDefinitions.map(_.toSet)
      filteredDefs         = defs.filterNot(d => d.isTestSupport || d.categories.contains(ApiCategory.EXAMPLE))
      ids                  = filteredDefs.flatMap(d => d.versionsAsList.filterNot(v => v.status == ApiStatus.RETIRED || v.status == ApiStatus.ALPHA).map(v => ApiIdentifier(d.context, v.versionNbr)))
      withAnyAdditionalIds = CdsVersionHandler.populateSpecialCases(ids)
    } yield withAnyAdditionalIds
  }
}
