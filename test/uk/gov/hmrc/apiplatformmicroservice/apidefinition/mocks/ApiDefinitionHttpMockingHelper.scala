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
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.connectors.ApiDefinitionConnectorUtils
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.connectors.ApiDefinitionConnectorUtils._
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.{APIDefinition, ExtendedAPIDefinition}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.http.ws.WSGet

import scala.concurrent.Future

trait ApiDefinitionHttpMockingHelper
    extends MockitoSugar
    with ArgumentMatchersSugar {
  val mockThisClient: HttpClient with WSGet
  val apiDefinitionUrl: String

  def whenGetDefinitionByEmail(serviceName: String, email: String)(
      definition: ExtendedAPIDefinition): Unit = {
    val url = extendedDefinitionUrl(apiDefinitionUrl, serviceName)
    when(
      mockThisClient.GET[ExtendedAPIDefinition](
        eqTo(url),
        eqTo(queryParams(Some(email)))
      )(any, any, any)
    ).thenReturn(Future.successful(definition))
  }

  def whenGetExtendedDefinition(serviceName: String)(
      definition: ExtendedAPIDefinition): Unit = {
    val url = extendedDefinitionUrl(apiDefinitionUrl, serviceName)
    when(
      mockThisClient.GET[ExtendedAPIDefinition](
        eqTo(url),
        eqTo(defaultParams)
      )(any, any, any)
    ).thenReturn(Future.successful(definition))
  }

  def whenGetDefinition(serviceName: String)(
      definition: APIDefinition): Unit = {
    val url = definitionUrl(apiDefinitionUrl, serviceName)
    when(
      mockThisClient.GET[APIDefinition](
        eqTo(url)
      )(any, any, any)
    ).thenReturn(Future.successful(definition))
  }

  def whenGetExtendedDefinitionFails(serviceName: String)(
      exception: Throwable): Unit = {
    val url =
      ApiDefinitionConnectorUtils.extendedDefinitionUrl(apiDefinitionUrl,
                                                        serviceName)
    when(
      mockThisClient.GET[ExtendedAPIDefinition](
        eqTo(url),
        eqTo(defaultParams)
      )(any, any, any)
    ).thenReturn(Future.failed(exception))
  }

  def whenGetDefinitionFails(serviceName: String)(
      exception: Throwable): Unit = {
    val url =
      ApiDefinitionConnectorUtils.definitionUrl(apiDefinitionUrl, serviceName)
    when(
      mockThisClient.GET[APIDefinition](
        eqTo(url)
      )(any, any, any)
    ).thenReturn(Future.failed(exception))
  }

  def whenGetAllDefinitions(definitions: APIDefinition*): Unit = {
    val url = ApiDefinitionConnectorUtils.definitionsUrl(apiDefinitionUrl)
    when(
      mockThisClient.GET[Seq[APIDefinition]](eqTo(url), eqTo(Seq("type" -> "all")))(any, any, any)
    ).thenReturn(Future.successful(definitions.toSeq))
  }

  def whenGetAllDefinitionsFails(exception: Throwable): Unit = {
    val url = definitionsUrl(apiDefinitionUrl)
    when(
      mockThisClient.GET[ExtendedAPIDefinition](eqTo(url), eqTo(Seq("type" -> "all")))(any, any, any)
    ).thenReturn(Future.failed(exception))
  }

  def whenGetAllDefinitionsWithoutFiltering(
      definitions: APIDefinition*): Unit = {
    val url = definitionsUrl(apiDefinitionUrl)
    when(
      mockThisClient.GET[Seq[APIDefinition]](
        eqTo(url),
        eqTo(Seq("type" -> "all"))
      )(any, any, any)
    ).thenReturn(Future.successful(definitions.toSeq))
  }
  def whenGetAllDefinitionsWithoutFilteringFails(exception: Throwable): Unit = {
    val url = definitionsUrl(apiDefinitionUrl)
    when(
      mockThisClient.GET[ExtendedAPIDefinition](
        eqTo(url),
        eqTo(Seq("type" -> "all"))
      )(any, any, any)
    ).thenReturn(Future.failed(exception))
  }
}
