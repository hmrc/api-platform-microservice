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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors

import scala.concurrent.ExecutionContext.Implicits.global

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatestplus.play.guice.GuiceOneServerPerSuite

import play.api.http.Status._
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2

import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApiContext, ApiIdentifier, ApiVersionNbr, ClientId}
import uk.gov.hmrc.apiplatformmicroservice.common.utils.{AsyncHmrcSpec, WireMockSugar, WireMockSugarExtensions}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.SubscriptionFieldsConnectorDomain.JsonFormatters._
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.SubscriptionFieldsConnectorDomain._
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.SubscriptionsHelper._
import uk.gov.hmrc.apiplatform.modules.applications.subscriptions.interface.models.BulkSubscriptionFieldsResponseFixtures
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApiIdentifierFixtures
import uk.gov.hmrc.apiplatform.modules.applications.subscriptions.domain.models.ApiFieldMapFixtures

class SubscriptionFieldsConnectorSpec
    extends AsyncHmrcSpec
    with WireMockSugar
    with WireMockSugarExtensions
    with GuiceOneServerPerSuite
    with BulkSubscriptionFieldsResponseFixtures with ApiIdentifierFixtures with ApiFieldMapFixtures {

  class SetupPrincipal {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val clientId                   = ClientId("123")

    val httpClient = app.injector.instanceOf[HttpClientV2]
    val config     = PrincipalSubscriptionFieldsConnector.Config(wireMockUrl)
    val connector  = new PrincipalSubscriptionFieldsConnector(config, httpClient)
  }

  "SubscriptionFieldsConnector" should {
    "retrieve all field values by client id" in new SetupPrincipal {
      stubFor(
        get(urlEqualTo(s"/field/application/${clientId}"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withJsonBody(bulkSubsOne)
          )
      )

      val expected = Map(
        apiContextOne -> Map(
          apiVersionNbrOne -> fieldMapOne,
          apiVersionNbrTwo -> fieldMapTwo
        ),
        apiContextTwo -> Map(
          apiVersionNbrOne -> fieldMapThree
        )
      )


      await(connector.bulkFetchFieldValues(clientId)) shouldBe expected
    }

    "save field values" should {
      "work with good values" in new SetupPrincipal {
        val request: SubscriptionFieldsPutRequest = SubscriptionFieldsPutRequest(clientId, ContextA, VersionOne, fieldMapOne)

        stubFor(
          put(urlEqualTo(s"/field/application/${clientId}/context/${ContextA}/version/${VersionOne}"))
            .withJsonRequestBody(request)
            .willReturn(
              aResponse()
                .withStatus(OK)
            )
        )

        val result = await(connector.saveFieldValues(clientId, ApiIdentifierAOne, fieldMapOne))

        result shouldBe Right(())
      }

      "return field errors with bad values" in new SetupPrincipal {
        val request: SubscriptionFieldsPutRequest = SubscriptionFieldsPutRequest(clientId, ContextA, VersionOne, fieldMapOne)
        val error                                 = "This is wrong"

        stubFor(
          put(urlEqualTo(s"/field/application/${clientId}/context/${ContextA}/version/${VersionOne}"))
            .withJsonRequestBody(request)
            .willReturn(
              aResponse()
                .withStatus(BAD_REQUEST)
                .withJsonBody(Map(FieldNameOne -> error))
            )
        )

        val result = await(connector.saveFieldValues(clientId, ApiIdentifierAOne, fieldMapOne))

        result shouldBe Left(Map(FieldNameOne -> error))
      }
    }
  }

  "return simple url" in new SetupPrincipal {
    connector.urlSubscriptionFieldValues(
      ClientId("1"),
      ApiIdentifier(ApiContext("path"), ApiVersionNbr("1"))
    ).toString shouldBe "http://localhost:22222/field/application/1/context/path/version/1"
  }
  "return complex encoded url" in new SetupPrincipal {
    connector.urlSubscriptionFieldValues(
      ClientId("1 2"),
      ApiIdentifier(ApiContext("path1/path2"), ApiVersionNbr("1.0 demo"))
    ).toString shouldBe "http://localhost:22222/field/application/1%202/context/path1%2Fpath2/version/1.0%20demo"
  }

}
