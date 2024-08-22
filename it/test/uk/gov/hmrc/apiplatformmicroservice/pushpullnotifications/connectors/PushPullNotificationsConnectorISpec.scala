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

package uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.connectors

import scala.concurrent.ExecutionContext.Implicits.global

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatestplus.play.guice.GuiceOneServerPerSuite

import play.api.http.Status._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2

import uk.gov.hmrc.apiplatform.modules.common.domain.models.ClientId
import uk.gov.hmrc.apiplatform.modules.common.domain.models.Environment.PRODUCTION
import uk.gov.hmrc.apiplatformmicroservice.common.builder._
import uk.gov.hmrc.apiplatformmicroservice.common.utils.{AsyncHmrcSpec, WireMockSugarExtensions}
import uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.builder.BoxBuilder
import uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.connectors.domain.BoxResponse
import uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.domain.{BoxCreator, BoxId, BoxSubscriber}
import uk.gov.hmrc.apiplatformmicroservice.utils.{ConfigBuilder, PrincipalAndSubordinateWireMockSetup}

class PushPullNotificationsConnectorISpec
    extends AsyncHmrcSpec
    with WireMockSugarExtensions
    with GuiceOneServerPerSuite
    with ConfigBuilder
    with PrincipalAndSubordinateWireMockSetup
    with ApplicationBuilder {

  trait Setup extends BoxBuilder {

    import play.api.libs.json._

    implicit val clientIdWrites: Format[ClientId] = Json.valueFormat[ClientId]

    implicit val boxCreatorWrites: Writes[BoxCreator]       = Json.writes[BoxCreator]
    implicit val boxSubscriberWrites: Writes[BoxSubscriber] = Json.writes[BoxSubscriber]
    implicit val boxIdWrites: Writes[BoxId]                 = Json.valueFormat[BoxId]
    implicit val boxResponseWrites: Writes[BoxResponse]     = Json.writes[BoxResponse]

    implicit val hc: HeaderCarrier = HeaderCarrier()
    val httpClient                 = app.injector.instanceOf[HttpClientV2]

    val config                                            = PrincipalPushPullNotificationsConnector.Config(
      applicationBaseUrl = s"http://$WireMockHost:$WireMockPrincipalPort"
    )
    val connector: AbstractPushPullNotificationsConnector = new PrincipalPushPullNotificationsConnector(config, httpClient)

  }

  "Get all boxes" should {
    val url = "/box"

    "return all boxes" in new Setup {
      val boxes = List(buildBoxResponse("1"))

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

    "return boxes with no applicationId" in new Setup {
      val boxes = List(buildBoxResponse(
        boxId = "1",
        applicationId = None
      ))

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
