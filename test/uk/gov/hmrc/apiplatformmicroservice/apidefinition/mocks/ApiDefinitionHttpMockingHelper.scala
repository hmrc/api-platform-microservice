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

package uk.gov.hmrc.apiplatformmicroservice.apidefinition.mocks

import scala.concurrent.Future

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.play.http.ws.WSGet

import uk.gov.hmrc.apiplatformmicroservice.apidefinition.connectors.ApiDefinitionConnectorUtils
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiDefinition


trait ApiDefinitionHttpMockingHelper
    extends MockitoSugar
    with ArgumentMatchersSugar
    with ApiDefinitionConnectorUtils {
  val mockThisClient: HttpClient with WSGet

  val apiDefinitionUrl: String

  private def whenGetDefinition(serviceName: String, response: Future[Option[ApiDefinition]]) = {
    val url = definitionUrl(serviceName)
    when(
      mockThisClient.GET[Option[ApiDefinition]](
        eqTo(url),
        *,
        *
      )(*, *, *)
    ).thenReturn(response)
  }

  def whenGetDefinition(serviceName: String)(definition: ApiDefinition): Unit = {
    whenGetDefinition(serviceName, Future.successful(Some(definition)))
  }

  def whenGetDefinitionFindsNothing(serviceName: String) = {
    whenGetDefinition(serviceName, Future.successful(None))
  }

  def whenGetDefinitionFails(serviceName: String)(exception: Throwable): Unit = {
    whenGetDefinition(serviceName, Future.failed(exception))
  }

  def whenGetAllDefinitions(definitions: ApiDefinition*): Unit = {
    val url = definitionsUrl
    when(
      mockThisClient.GET[Option[List[ApiDefinition]]](eqTo(url), eqTo(Seq("type" -> "all")), *)(*, *, *)
    ).thenReturn(Future.successful(Some(definitions.toList)))
  }

  def whenGetAllDefinitionsFindsNothing(): Unit = {
    val url = definitionsUrl
    when(
      mockThisClient.GET[Option[List[ApiDefinition]]](eqTo(url), eqTo(Seq("type" -> "all")), *)(*, *, *)
    ).thenReturn(Future.successful(None))
  }

  def whenGetAllDefinitionsFails(exception: Throwable): Unit = {
    val url = definitionsUrl
    when(
      mockThisClient.GET[Option[List[ApiDefinition]]](eqTo(url), eqTo(Seq("type" -> "all")), *)(*, *, *)
    ).thenReturn(Future.failed(exception))
  }
}
