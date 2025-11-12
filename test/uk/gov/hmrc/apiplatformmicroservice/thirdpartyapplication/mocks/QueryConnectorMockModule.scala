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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks

import scala.concurrent.Future.{failed, successful}

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.Environment
import uk.gov.hmrc.apiplatform.modules.applications.query.domain.models.ApplicationQuery
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.QueryConnector

trait QueryConnectorMockModule extends MockitoSugar with ArgumentMatchersSugar {

  trait AbstractQueryConnectMock {
    def aMock: QueryConnector

    object ByQuery {

      def returnsFor[T](env: Environment, query: ApplicationQuery, results: T) = {
        when(aMock.query[T](eqTo(env), eqTo(query))(*, *)).thenReturn(successful(results))
      }

      def failsFor[T](env: Environment, query: ApplicationQuery, err: Throwable) = {
        when(aMock.query[T](eqTo(env), eqTo(query))(*, *)).thenReturn(failed(err))
      }

      def returns[T](results: T) = {
        when(aMock.query[T](*[Environment], *[ApplicationQuery])(*, *)).thenReturn(successful(results))
      }

      def fails[T](err: Throwable) = {
        when(aMock.query[T](*[Environment], *[ApplicationQuery])(*, *)).thenReturn(failed(err))
      }
    }

    object ByQueryParams {

      def returns[T](results: T) = {
        when(aMock.query[T](*[Environment], *[Map[String, Seq[String]]])(*, *)).thenReturn(successful(results))
      }

      def fails[T](err: Throwable) = {
        when(aMock.query[T](*[Environment], *[Map[String, Seq[String]]])(*, *)).thenReturn(failed(err))
      }
    }
  }

  object QueryConnectorMock extends AbstractQueryConnectMock {
    override val aMock: QueryConnector = mock[QueryConnector]
  }
}
