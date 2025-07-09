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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication

import com.github.tomakehurst.wiremock.client.WireMock._

import play.api.http.Status._
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.utils.PrincipalAndSubordinateWireMockSetup
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.controllers.SubscriptionFieldsController.Fields

trait SubscriptionFieldsMock {
  self: PrincipalAndSubordinateWireMockSetup => // To allow for stubFor to work with environment

  def mockUpsertNotFound(env: Environment): Unit = {
    stubFor(env)(put(urlMatching("""/field/application/[^/]+/context/[^/]+/version/[^/]+"""))
      .withRequestBody(matchingJsonPath("$.fields"))
      .willReturn(
        aResponse()
          .withStatus(NOT_FOUND)
      ))
  }

  // def mockUpsert(deployedTo: Environment, fields: Fields): Unit = {
  //   stubFor(deployedTo)(get(urlEqualTo(s"/developer/$userId/applications"))
  //     .willReturn(
  //       aResponse()
  //         .withBody(Json.toJson(
  //         ).toString())
  //         .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
  //         .withStatus(OK)
  //     ))
  // }
}
