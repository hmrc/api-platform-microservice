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

package uk.gov.hmrc.apiplatformmicroservice.combinedapis.services

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.common.domain.models.UserId
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ServiceName
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.services.{AllApisFetcher, ApiDefinitionsForCollaboratorFetcher, ExtendedApiDefinitionForCollaboratorFetcher}
import uk.gov.hmrc.apiplatformmicroservice.combinedapis.models.CombinedApi
import uk.gov.hmrc.apiplatformmicroservice.combinedapis.utils.CombinedApiDataHelper.{filterOutRetiredApis, fromApiDefinition, fromXmlApi}
import uk.gov.hmrc.apiplatformmicroservice.xmlapis.connectors.XmlApisConnector

@Singleton
class CombinedApisService @Inject() (
    apiDefinitionsForCollaboratorFetcher: ApiDefinitionsForCollaboratorFetcher,
    extendedApiDefinitionForCollaboratorFetcher: ExtendedApiDefinitionForCollaboratorFetcher,
    xmlApisConnector: XmlApisConnector,
    allApisFetcher: AllApisFetcher
  )(implicit ec: ExecutionContext
  ) {

  def fetchCombinedApisForDeveloperId(userId: Option[UserId])(implicit hc: HeaderCarrier): Future[List[CombinedApi]] = {
    for {
      restApis <- apiDefinitionsForCollaboratorFetcher.fetch(userId)
      xmlApis  <- xmlApisConnector.fetchAllXmlApis()
    } yield restApis.map(fromApiDefinition) ++ xmlApis.map(fromXmlApi)
  }

  def fetchAllCombinedApis()(implicit hc: HeaderCarrier): Future[List[CombinedApi]] = {
    for {
      restApis <- allApisFetcher.fetch().map(filterOutRetiredApis)
      xmlApis  <- xmlApisConnector.fetchAllXmlApis()
    } yield restApis.map(fromApiDefinition).distinct ++ xmlApis.map(fromXmlApi)
  }

  def fetchCombinedApiByServiceName(serviceName: ServiceName)(implicit hc: HeaderCarrier): Future[Option[CombinedApi]] = {
    def filterApis(apis: List[CombinedApi]): Option[CombinedApi] = apis.find(_.serviceName == serviceName)

    fetchAllCombinedApis().map(filterApis)
  }
}
