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

import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.{ApiCategory, ApiDefinition, ApiDefinitionTestDataHelper}
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.services.ApiDefinitionsForCollaboratorFetcher
import uk.gov.hmrc.apiplatformmicroservice.combinedapis.utils.CombinedApiDataHelper.{fromApiDefinition, fromXmlApi}
import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.DeveloperIdentifier
import uk.gov.hmrc.apiplatformmicroservice.xmlapis.connectors.XmlApisConnector
import uk.gov.hmrc.apiplatformmicroservice.xmlapis.models.XmlApi
import uk.gov.hmrc.http.HeaderCarrier

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CombinedApisServiceSpec extends AsyncHmrcSpec with ApiDefinitionTestDataHelper {

  trait SetUp {
    implicit val hc = mock[HeaderCarrier]
    val mockApiDefinitionsForCollaboratorFetcher = mock[ApiDefinitionsForCollaboratorFetcher]
    val mockXmlApisConnector = mock[XmlApisConnector]
    val objInTest = new CombinedApisService(mockApiDefinitionsForCollaboratorFetcher, mockXmlApisConnector)
    val developerId = DeveloperIdentifier(UUID.randomUUID().toString)

    val apiDefinition1 = apiDefinition(name = "service1").copy(categories = List(ApiCategory("OTHER"), ApiCategory("INCOME_TAX_MTD")))
    val apiDefinition2 = apiDefinition(name = "service2").copy( categories = List(ApiCategory("VAT"), ApiCategory("OTHER")))
    val listOfDefinitions = List(apiDefinition1, apiDefinition2)

    val xmlApi1 = XmlApi("xmlService1", "context", "desc", Some(List(ApiCategory("SELF_ASSESSMENT"), ApiCategory("CUSTOMS"))))
    val xmlApi2 = XmlApi("xmlService2", "context", "desc", Some(List(ApiCategory("OTHER"), ApiCategory("CUSTOMS"))))
    val xmlApis = List(xmlApi1, xmlApi2)

    val combinedList = List(fromApiDefinition(apiDefinition1), fromApiDefinition(apiDefinition2), fromXmlApi(xmlApi1), fromXmlApi(xmlApi2))
    def primeApiDefinitionsForCollaboratorFetcher(developerIdentifier: Option[DeveloperIdentifier], apisToReturn: List[ApiDefinition]) ={
      when(mockApiDefinitionsForCollaboratorFetcher.fetch(eqTo(developerIdentifier))(*))
        .thenReturn(Future.successful(apisToReturn))
    }

    def primeAXmlConnector(xmlApis: List[XmlApi]) ={
      when(mockXmlApisConnector.fetchAllXmlApis()(*)).thenReturn(Future.successful(xmlApis))
    }
  }
  "CombinedApisService" should {
    "return combined list of xml and rest apis" in new SetUp {
      primeApiDefinitionsForCollaboratorFetcher(developerId, listOfDefinitions)
      primeAXmlConnector(xmlApis)

      val result = await(objInTest.fetchCombinedApisForDeveloperId(developerId))
      result.size shouldBe 4
      result shouldBe combinedList
    }
  }

}
