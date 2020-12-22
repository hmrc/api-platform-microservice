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

package uk.gov.hmrc.apiplatformmicroservice.apidefinition.mocks

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.connectors.ApiDefinitionConnectorUtils._
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.{APICategoryDetails, APIDefinition}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.http.ws.WSGet

import scala.concurrent.Future

trait ApiDefinitionHttpMockingHelper
    extends MockitoSugar
    with ArgumentMatchersSugar {
  val mockThisClient: HttpClient with WSGet
  val apiDefinitionUrl: String

  private def whenGetDefinition(serviceName: String, response: Future[Option[APIDefinition]]) = {
    val url = definitionUrl(apiDefinitionUrl, serviceName)
    when(
      mockThisClient.GET[Option[APIDefinition]](
        eqTo(url)
      )(any, any, any)
    ).thenReturn(response)
  }

  def whenGetDefinition(serviceName: String)(definition: APIDefinition): Unit = {
    whenGetDefinition(serviceName, Future.successful(Some(definition)))
  }

  def whenGetDefinitionFindsNothing(serviceName: String) = {
    whenGetDefinition(serviceName, Future.successful(None))
  }

  def whenGetDefinitionFails(serviceName: String)(exception: Throwable): Unit = {
    whenGetDefinition(serviceName, Future.failed(exception))
  }

  def whenGetAllDefinitions(definitions: APIDefinition*): Unit = {
    val url = definitionsUrl(apiDefinitionUrl)
    when(
      mockThisClient.GET[Option[List[APIDefinition]]](eqTo(url), eqTo(Seq("type" -> "all")))(any, any, any)
    ).thenReturn(Future.successful(Some(definitions.toList)))
  }

  def whenGetAllDefinitionsFindsNothing(): Unit = {
    val url = definitionsUrl(apiDefinitionUrl)
    when(
      mockThisClient.GET[Option[List[APIDefinition]]](eqTo(url), eqTo(Seq("type" -> "all")))(any, any, any)
    ).thenReturn(Future.successful(None))
  }

  def whenGetAllDefinitionsFails(exception: Throwable): Unit = {
    val url = definitionsUrl(apiDefinitionUrl)
    when(
      mockThisClient.GET[Option[List[APIDefinition]]](eqTo(url), eqTo(Seq("type" -> "all")))(any, any, any)
    ).thenReturn(Future.failed(exception))
  }

  def whenGetAPICategoryDetails()(categories: APICategoryDetails*): Unit = {
    val url = categoriesUrl(apiDefinitionUrl)
    when(mockThisClient.GET[Seq[APICategoryDetails]](eqTo(url))(any, any, any)).thenReturn(Future.successful(categories))
  }

  def whenGetAPICategoryDetailsFails(exception: Throwable): Unit = {
    val url = categoriesUrl(apiDefinitionUrl)
    when(mockThisClient.GET[Seq[APICategoryDetails]](eqTo(url))(any, any, any)).thenReturn(Future.failed(exception))
  }
}
