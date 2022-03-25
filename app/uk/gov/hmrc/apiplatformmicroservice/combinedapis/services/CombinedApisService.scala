/*
 * Copyright 2022 HM Revenue & Customs
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

import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.{ExtendedApiDefinition, OpenAccessRules}
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.services.{ApiDefinitionsForCollaboratorFetcher, ExtendedApiDefinitionForCollaboratorFetcher}
import uk.gov.hmrc.apiplatformmicroservice.combinedapis.models.CombinedApi
import uk.gov.hmrc.apiplatformmicroservice.combinedapis.utils.CombinedApiDataHelper.{filterOutRetiredApis, fromApiDefinition, fromExtendedApiDefinition, fromXmlApi}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.DeveloperIdentifier
import uk.gov.hmrc.apiplatformmicroservice.combinedapis.utils.CombinedApiDataHelper.{fromApiDefinition, fromExtendedApiDefinition, fromXmlApi}
import uk.gov.hmrc.apiplatformmicroservice.xmlapis.connectors.XmlApisConnector
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.services.AllApisFetcher
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.UserId

@Singleton
class CombinedApisService @Inject()(apiDefinitionsForCollaboratorFetcher: ApiDefinitionsForCollaboratorFetcher,
                                    extendedApiDefinitionForCollaboratorFetcher: ExtendedApiDefinitionForCollaboratorFetcher,
                                    xmlApisConnector: XmlApisConnector,
                                    allApisFetcher: AllApisFetcher)
                                   (implicit ec: ExecutionContext) extends OpenAccessRules {


  def fetchCombinedApisForDeveloperId(userId: Option[UserId])
                                     (implicit hc: HeaderCarrier): Future[List[CombinedApi]] = {
    for {
      restApis <- apiDefinitionsForCollaboratorFetcher.fetch(userId)
      xmlApis <- xmlApisConnector.fetchAllXmlApis()
    } yield restApis.map(fromApiDefinition).distinct ++ xmlApis.map(fromXmlApi)
  }

  @deprecated("please use fetchCombinedApiByServiceName", "2022")
  def fetchApiForCollaborator(serviceName: String, userId: Option[UserId])
                             (implicit hc: HeaderCarrier): Future[Option[CombinedApi]]= {
    extendedApiDefinitionForCollaboratorFetcher.fetch(serviceName, userId) flatMap  {
      case Some(y: ExtendedApiDefinition) => Future.successful(Some(fromExtendedApiDefinition(y)))
      case _ => xmlApisConnector.fetchXmlApiByServiceName(serviceName).map(_.map(fromXmlApi))
    }
  }

  def fetchAllCombinedApis()(implicit hc: HeaderCarrier): Future[List[CombinedApi]] = {
    for {
      restApis <- allApisFetcher.fetch().map(filterOutRetiredApis)
      xmlApis <- xmlApisConnector.fetchAllXmlApis()
    } yield restApis.map(fromApiDefinition) ++ xmlApis.map(fromXmlApi)
  }

  def fetchCombinedApiByServiceName(serviceName: String)(implicit hc: HeaderCarrier): Future[Option[CombinedApi]] = {
    def filterApis(apis: List[CombinedApi]): Option[CombinedApi] = apis.find(_.serviceName == serviceName)

    fetchAllCombinedApis.map(filterApis)
  }
}
