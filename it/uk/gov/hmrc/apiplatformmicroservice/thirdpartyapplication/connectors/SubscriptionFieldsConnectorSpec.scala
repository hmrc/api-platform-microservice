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

import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec

import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.SubscriptionFieldsConnectorDomain._
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.ClientId
import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.apiplatformmicroservice.common.utils.{WireMockSugar, WireMockSugarExtensions}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.SubscriptionsHelper._
import uk.gov.hmrc.http.HeaderCarrier

class SubscriptionFieldsConnectorSpec
    extends AsyncHmrcSpec 
    with WireMockSugar 
    with WireMockSugarExtensions 
    with GuiceOneServerPerSuite
    with SubscriptionFieldValuesJsonFormatters {

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
    val clientId = ClientId("123")

    val httpClient = app.injector.instanceOf[HttpClient]
    val config = PrincipalSubscriptionFieldsConnector.Config(wireMockUrl)
    val connector = new PrincipalSubscriptionFieldsConnector(config, httpClient)
  }

  "SubscriptionFieldsConnector" should {
    "retrieve all field values by client id" in new SetupPrincipal {
      implicit val writes = Json.writes[BulkSubscriptionFieldsResponse]

      stubFor(
        get(urlEqualTo(s"/field/application/${clientId.value}"))
        .willReturn(
          aResponse()
          .withStatus(OK)
          .withJsonBody(BulkSubscriptionFieldsResponse(bulkSubscriptions))
        )
      )

      await(connector.bulkFetchFieldValues(clientId)) shouldBe subsFields
    }
  }
}
