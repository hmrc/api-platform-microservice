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

package uk.gov.hmrc.apiplatformmicroservice.apidefinition.controllers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiDefinitionTestDataHelper
import uk.gov.hmrc.apiplatformmicroservice.common.StreamedResponseHelper.PROXY_SAFE_CONTENT_TYPE
import uk.gov.hmrc.apiplatformmicroservice.util.AsyncHmrcSpec
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks._

import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.controllers.ApplicationController

class ApplicationControllerSpec extends AsyncHmrcSpec with GuiceOneAppPerSuite with ApiDefinitionTestDataHelper {

  trait Setup extends ApplicationByIdFetcherModule with SubscriptionServiceModule {
    implicit val headerCarrier = HeaderCarrier()
    implicit val system = ActorSystem("test")
    implicit val mat = ActorMaterializer()

    val request = FakeRequest("GET", "/")
    val apiName = "hello-api"
    val version = "1.0"
    val anApiDefinition = apiDefinition(apiName)
    val anExtendedApiDefinition = extendedApiDefinition(apiName)

    val controller = new ApplicationController(
      SubscriptionServiceMock.aMock,
      ApplicationByIdFetcherMock.aMock,
      Helpers.stubControllerComponents()
    )
    val mockWSResponse = mock[WSResponse]
    when(mockWSResponse.status).thenReturn(OK)
    when(mockWSResponse.headers).thenReturn(
      Map(
        "Content-Length" -> Seq("500")
      )
    )
    when(mockWSResponse.header(eqTo(PROXY_SAFE_CONTENT_TYPE))).thenReturn(None)
    when(mockWSResponse.contentType).thenReturn("application/json")
  }


}
