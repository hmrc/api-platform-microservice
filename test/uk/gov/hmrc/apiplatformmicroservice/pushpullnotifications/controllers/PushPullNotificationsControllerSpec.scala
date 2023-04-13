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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.controllers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future.successful

import akka.actor.ActorSystem
import akka.stream.Materializer
import org.scalatest.BeforeAndAfterAll

import play.api.libs.json.Json
import play.api.test.Helpers.{status, _}
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiDefinitionTestDataHelper
import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.builder.BoxBuilder
import uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.controllers.PushPullNotificationsController
import uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.domain.services.PushPullNotificationJsonFormatters
import uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.services.BoxFetcher

class PushPullNotificationsControllerSpec extends AsyncHmrcSpec with BeforeAndAfterAll with ApiDefinitionTestDataHelper with PushPullNotificationJsonFormatters {

  var as: ActorSystem            = _
  implicit var mat: Materializer = _

  override protected def beforeAll(): Unit = {
    as = ActorSystem("test")
    mat = Materializer(as)
  }

  override protected def afterAll(): Unit = {
    mat = null
    await(as.terminate())
    as = null
  }

  trait Setup extends BoxBuilder {
    implicit val headerCarrier = HeaderCarrier()
    val mockBoxFetcher         = mock[BoxFetcher](org.mockito.Mockito.withSettings().verboseLogging())

    val controller = new PushPullNotificationsController(
      mockBoxFetcher,
      Helpers.stubControllerComponents()
    )
  }

  "GET /boxes" should {
    "return a list of boxes" in new Setup {
      val boxes = List(buildBox("1"))

      when(mockBoxFetcher.fetchAllBoxes()(*)).thenReturn(successful(boxes))

      val result = controller.getAll()(FakeRequest("GET", "/"))

      status(result) shouldBe OK
      contentAsString(result) shouldBe Json.toJson(boxes).toString()
    }
  }
}
