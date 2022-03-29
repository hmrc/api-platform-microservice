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

import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.{ApiCategory, ApiDefinition, ApiDefinitionTestDataHelper, ExtendedApiDefinition}
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.services.{ApiDefinitionsForCollaboratorFetcher, ExtendedApiDefinitionForCollaboratorFetcher}
import uk.gov.hmrc.apiplatformmicroservice.combinedapis.utils.CombinedApiDataHelper.{fromApiDefinition, fromExtendedApiDefinition, fromXmlApi}
import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.apiplatformmicroservice.xmlapis.connectors.XmlApisConnector
import uk.gov.hmrc.apiplatformmicroservice.xmlapis.models.XmlApi
import uk.gov.hmrc.http.HeaderCarrier

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.services.AllApisFetcher
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.UserId

class CombinedApisServiceSpec extends AsyncHmrcSpec with ApiDefinitionTestDataHelper {

  trait SetUp {
    implicit val hc = mock[HeaderCarrier]
    val mockApiDefinitionsForCollaboratorFetcher = mock[ApiDefinitionsForCollaboratorFetcher]
    val mockExtendedApiDefinitionForCollaboratorFetcher = mock[ExtendedApiDefinitionForCollaboratorFetcher]
    val mockXmlApisConnector = mock[XmlApisConnector]
    val mockAllApisFetcher = mock[AllApisFetcher]
    val inTest = new CombinedApisService(mockApiDefinitionsForCollaboratorFetcher, mockExtendedApiDefinitionForCollaboratorFetcher, mockXmlApisConnector, mockAllApisFetcher)
    val developerId = Some(UserId.random)

    val apiDefinition1 = apiDefinition(name = "service1").copy(categories = List(ApiCategory("OTHER"), ApiCategory("INCOME_TAX_MTD")))
    val apiDefinition2 = apiDefinition(name = "service2").copy( categories = List(ApiCategory("VAT"), ApiCategory("OTHER")))
    val listOfDefinitions = List(apiDefinition1, apiDefinition2)

    val extendedApiDefinition1 = extendedApiDefinition("service-name1")

    val xmlApi1 = XmlApi("xmlService1", "xml-service-1", "context", "desc", Some(List(ApiCategory("SELF_ASSESSMENT"), ApiCategory("CUSTOMS"))))
    val xmlApi2 = XmlApi("xmlService2", "xml-service-2", "context", "desc", Some(List(ApiCategory("OTHER"), ApiCategory("CUSTOMS"))))
    val xmlApis = List(xmlApi1, xmlApi2)

    val combinedList = List(fromApiDefinition(apiDefinition1), fromApiDefinition(apiDefinition2), fromXmlApi(xmlApi1), fromXmlApi(xmlApi2))
    def primeApiDefinitionsForCollaboratorFetcher(developerIdentifier: Option[UserId], apisToReturn: List[ApiDefinition]) ={
      when(mockApiDefinitionsForCollaboratorFetcher.fetch(eqTo(developerIdentifier))(*))
        .thenReturn(Future.successful(apisToReturn))
    }

    def primeExtendedApiDefinitionForCollaboratorFetcher(serviceName: String, developerIdentifier: Option[UserId], apiToReturn: Option[ExtendedApiDefinition]) = {
      when(mockExtendedApiDefinitionForCollaboratorFetcher.fetch(eqTo(serviceName), eqTo(developerIdentifier))(*))
        .thenReturn(Future.successful(apiToReturn))
    }

    def primeXmlConnectorFetchAll(xmlApis: List[XmlApi]) ={
      when(mockXmlApisConnector.fetchAllXmlApis()(*)).thenReturn(Future.successful(xmlApis))
    }
    def primeXmlConnectorFetchByServiceName(serviceName: String, xmlApis: Option[XmlApi]) ={
      when(mockXmlApisConnector.fetchXmlApiByServiceName(eqTo(serviceName))(*)).thenReturn(Future.successful(xmlApis))
    }

  }
  "CombinedApisService" when {

    "fetchCombinedApisForDeveloperId" should {

      "return combined list of xml and rest apis" in new SetUp {
        primeApiDefinitionsForCollaboratorFetcher(developerId, listOfDefinitions)
        primeXmlConnectorFetchAll(xmlApis)

        val result = await(inTest.fetchCombinedApisForDeveloperId(developerId))
        result.size shouldBe 4
        result shouldBe combinedList
      }
    }
    "fetchApiForCollaborator" should {

      "return exact matched api from api definition if matched" in new SetUp {
        val serviceName = "service-name"

        primeExtendedApiDefinitionForCollaboratorFetcher(serviceName, developerId, Some(extendedApiDefinition1))


        val result = await(inTest.fetchApiForCollaborator(serviceName, developerId))
        result.size shouldBe 1
        result shouldBe Some(fromExtendedApiDefinition(extendedApiDefinition1))

        verifyZeroInteractions(mockXmlApisConnector)
      }

      "return matched api from api definition if matched" in new SetUp {
        val serviceName = "service-name"

        primeExtendedApiDefinitionForCollaboratorFetcher(serviceName, developerId, None)
        primeXmlConnectorFetchByServiceName(serviceName, Some(xmlApi1))

        val result = await(inTest.fetchApiForCollaborator(serviceName, developerId))
        result.size shouldBe 1
        result shouldBe Some(fromXmlApi(xmlApi1))

      }
    }

    "fetchAllCombinedApis" should {

      "return combined list of apis when both services return results" in new SetUp {
          primeXmlConnectorFetchAll(xmlApis)
          when(mockAllApisFetcher.fetch()(*)).thenReturn(Future.successful(listOfDefinitions))
          

          val result = await(inTest.fetchAllCombinedApis())
          result should contain only (combinedList: _*)

      }

      "return combined list of apis when only xml service returns results" in new SetUp {
          primeXmlConnectorFetchAll(xmlApis)
          when(mockAllApisFetcher.fetch()(*)).thenReturn(Future.successful(List.empty))

          val expectedResults = xmlApis.map(fromXmlApi)
            
          val result = await(inTest.fetchAllCombinedApis())
          result should contain only (expectedResults: _*)
      }

      "return combined list of apis when only AllApisFetcher returns results" in new SetUp {
          primeXmlConnectorFetchAll(List.empty)
          when(mockAllApisFetcher.fetch()(*)).thenReturn(Future.successful(listOfDefinitions))

          val expectedResults = listOfDefinitions.map(fromApiDefinition)

          val result = await(inTest.fetchAllCombinedApis())
          result should contain only (expectedResults: _*)        
      }

      
      "return empty list of apis when no results returned from either service" in new SetUp {
          primeXmlConnectorFetchAll(List.empty)
          when(mockAllApisFetcher.fetch()(*)).thenReturn(Future.successful(List.empty))

          val result = await(inTest.fetchAllCombinedApis())
          result shouldBe Nil        
      }

    }

  }

}
