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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors._

import scala.concurrent.Future.{failed, successful}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.apiplatform.modules.subscriptions.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiIdentifier

trait SubscriptionFieldsConnectorModule {
  self: MockitoSugar with ArgumentMatchersSugar =>

  abstract class SubscriptionFieldsConnectorMock {
    def aMock: SubscriptionFieldsConnector

    object BulkFetchFieldValues {

      def willReturnFields(subs: ApiFieldMap[FieldValue])(implicit hc: HeaderCarrier) = {
        when(aMock.bulkFetchFieldValues(*[ClientId])(eqTo(hc))).thenReturn(successful(subs))
      }

      def willThrowException(e: Exception) =
        when(aMock.bulkFetchFieldValues(*[ClientId])(*[HeaderCarrier])).thenReturn(failed(e))
    }

    object BulkFetchFieldDefinitions {
      def willReturnDefinitions(defns: ApiFieldMap[FieldDefinition]) = {
        when(aMock.bulkFetchFieldDefinitions(*)).thenReturn(successful(defns))
      }
    }

    object SaveFieldValues {
      def willReturn(apiIdentifier: ApiIdentifier) = {
        when(aMock.saveFieldValues(*[ClientId], eqTo(apiIdentifier), *)(*)).thenReturn(successful(Right(())))
      }
    }
  }

  object SubordinateSubscriptionFieldsConnectorMock extends SubscriptionFieldsConnectorMock {
    override val aMock = mock[SubscriptionFieldsConnector]
  }

  object PrincipalSubscriptionFieldsConnectorMock extends SubscriptionFieldsConnectorMock {
    override val aMock = mock[SubscriptionFieldsConnector]
  }

  object EnvironmentAwareSubscriptionFieldsConnectorMock {
    private val subordinateConnector = SubordinateSubscriptionFieldsConnectorMock
    private val principalConnector = PrincipalSubscriptionFieldsConnectorMock

    lazy val instance = new EnvironmentAwareSubscriptionFieldsConnector(subordinateConnector.aMock, principalConnector.aMock)

    lazy val Principal = principalConnector

    lazy val Subordinate = subordinateConnector
  }
}
