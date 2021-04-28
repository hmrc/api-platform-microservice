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
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.Environment
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiStatus.RETIRED

@Singleton
class OpenAccessApisFetcher @Inject() (
    apiDefinitionService: EnvironmentAwareApiDefinitionService
  )(implicit ec: ExecutionContext) extends OpenAccessRules {

  private def filterOutRetiredVersions(definition: ApiDefinition): Option[ApiDefinition] = {
    val filteredVersions = definition.versions.filterNot(_.status == RETIRED)
    if(filteredVersions.isEmpty) None else Some(definition.copy(versions = filteredVersions))
  }

  def fetchAllForEnvironment(environment: Environment)(implicit hc: HeaderCarrier): Future[List[ApiDefinition]] = {
    import cats.data.Nested
    import cats.implicits._

    Nested(apiDefinitionService(environment).fetchAllOpenAccessApiDefinitions)
    .map(filterOutRetiredVersions)
    .collect({ case Some(x) => x})
    .value
  }
}
