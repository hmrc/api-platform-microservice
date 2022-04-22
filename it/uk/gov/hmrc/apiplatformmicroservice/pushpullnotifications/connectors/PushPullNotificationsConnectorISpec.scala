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

package uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.apiplatformmicroservice.common.ProxiedHttpClient
import uk.gov.hmrc.apiplatformmicroservice.common.builder._
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.Environment.PRODUCTION
import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.apiplatformmicroservice.common.utils.WireMockSugarExtensions
import uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.connectors.domain.BoxResponse
import uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.domain.BoxCreator
import uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.domain.BoxSubscriber
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.ClientId
import uk.gov.hmrc.apiplatformmicroservice.utils.ConfigBuilder
import uk.gov.hmrc.apiplatformmicroservice.utils.PrincipalAndSubordinateWireMockSetup
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global

class PushPullNotificationsConnectorISpec
    extends AsyncHmrcSpec
    with WireMockSugarExtensions
    with GuiceOneServerPerSuite
    with ConfigBuilder
    with PrincipalAndSubordinateWireMockSetup
    with ApplicationBuilder {

  trait Setup {

    import play.api.libs.json._  
    import play.api.libs.json.JodaWrites._


    implicit val clientIdWrites = Json.valueFormat[ClientId]
    
    implicit val boxCreatorWrites = Json.writes[BoxCreator]
    implicit val boxSubscriberWrites = Json.writes[BoxSubscriber]

    implicit val boxResponseWrites = Json.writes[BoxResponse]

    implicit val hc = HeaderCarrier()
    val httpClient = app.injector.instanceOf[HttpClient]
    protected val mockProxiedHttpClient = mock[ProxiedHttpClient]
    val apiKeyTest = "5bb51bca-8f97-4f2b-aee4-81a4a70a42d3"
    val bearer = "TestBearerToken"


    val config = AbstractPushPullNotificationsConnector.Config(
      applicationBaseUrl = s"http://$WireMockHost:$WireMockPrincipalPort",
      applicationUseProxy = false,
      applicationBearerToken = bearer,
      applicationApiKey = apiKeyTest
    )
    val connector: AbstractPushPullNotificationsConnector = new PrincipalPushPullNotificationsConnector(config, httpClient, mockProxiedHttpClient)
  }

  trait SubordinateSetup extends Setup {
    override val config = AbstractPushPullNotificationsConnector.Config(
      applicationBaseUrl = s"http://$WireMockHost:$WireMockSubordinatePort",
      applicationUseProxy = false,
      applicationBearerToken = bearer,
      applicationApiKey = apiKeyTest
    )
    override val connector = new SubordinatePushPullNotificationsConnector(config, httpClient, mockProxiedHttpClient)
  }

  "Get all boxes" should {
    val url = "/box"
    
    "return all boxes" in new Setup {

      val boxes = List[BoxResponse](BoxResponse("boxId","boxName", BoxCreator(ClientId(java.util.UUID.randomUUID().toString())),ApplicationId.random, None))

      stubFor(PRODUCTION)(
        get(urlEqualTo(url))
        .willReturn(
          aResponse()
          .withStatus(OK)
          .withJsonBody(boxes)
        )
      )
      val boxResponse = await(connector.fetchAllBoxes())

      boxResponse shouldBe boxes
    }
  }
}
