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

package uk.gov.hmrc.apiplatformmicroservice.combinedapis.services

import uk.gov.hmrc.apiplatformmicroservice.apidefinition.services.ApiDefinitionsForCollaboratorFetcher
import uk.gov.hmrc.apiplatformmicroservice.combinedapis.models.CombinedApi
import uk.gov.hmrc.apiplatformmicroservice.combinedapis.utils.CombinedApiDataHelper.{fromApiDefinition, fromXmlApi}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.DeveloperIdentifier
import uk.gov.hmrc.apiplatformmicroservice.xmlapis.connectors.XmlApisConnector
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CombinedApisService @Inject()(apiDefinitionsForCollaboratorFetcher: ApiDefinitionsForCollaboratorFetcher, xmlApisConnector: XmlApisConnector)
                                   (implicit ec: ExecutionContext) {



  def fetchCombinedApisForDeveloperId(developerId: Option[DeveloperIdentifier])
                                     (implicit hc: HeaderCarrier): Future[List[CombinedApi]] =
    for{
      restApis <- apiDefinitionsForCollaboratorFetcher.fetch(developerId)
      xmlApis <- xmlApisConnector.fetchAllXmlApis()
    } yield restApis.map(fromApiDefinition) ++ xmlApis.map(fromXmlApi)


}


