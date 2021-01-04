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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors

import uk.gov.hmrc.apiplatformmicroservice.util.AsyncHmrcSpec
import uk.gov.hmrc.http.HeaderCarrier
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import org.mockito.MockitoSugar
import org.mockito.ArgumentMatchersSugar
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.SubscriptionFieldsConnectorDomain._
import scala.concurrent.Future.successful
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.ClientId

class SubscriptionFieldsConnectorSpec extends AsyncHmrcSpec with MockitoSugar with ArgumentMatchersSugar {
  import SubscriptionsHelper._

  private val baseUrl = "https://example.com"

  val fieldsForAOne = FieldNameOne -> "oneValue".asFieldValue
  val fieldsForATwo = FieldNameTwo -> "twoValue".asFieldValue
  val fieldsForBOne = FieldNameTwo -> "twoValueB".asFieldValue

  val subsFields =
    Map(
      ContextA -> Map(
        VersionOne -> Map(fieldsForAOne),
        VersionTwo -> Map(fieldsForATwo)
      ),
      ContextB -> Map(
        VersionOne -> Map(fieldsForBOne)
      )
    )

  val bulkSubscriptions = Seq(
    SubscriptionFields(ContextA, VersionOne, Map(fieldsForAOne)),
    SubscriptionFields(ContextA, VersionTwo, Map(fieldsForATwo)),
    SubscriptionFields(ContextB, VersionOne, Map(fieldsForBOne))
  )

  class SetupPrincipal {
    implicit val hc = HeaderCarrier()
    protected val mockHttpClient = mock[HttpClient]

    val clientId = ClientId("123")

    val config = PrincipalSubscriptionFieldsConnector.Config(
      baseUrl
    )

    val connector = new PrincipalSubscriptionFieldsConnector(config, mockHttpClient)
  }

  "SubscriptionFieldsConnector" should {
    "retrieve all field values by client id" in new SetupPrincipal {
      when(mockHttpClient.GET[Option[BulkSubscriptionFieldsResponse]](*)(*, *, *))
        .thenReturn(successful(Some(BulkSubscriptionFieldsResponse(bulkSubscriptions))))

      await(connector.bulkFetchFieldValues(clientId)) shouldBe subsFields
    }
  }
}
