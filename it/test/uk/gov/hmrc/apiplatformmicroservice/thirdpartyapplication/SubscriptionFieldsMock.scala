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
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json.Json

import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.subscriptionfields.domain.models.{FieldErrorMap, FieldNameFixtures, FieldsFixtures}
import uk.gov.hmrc.apiplatformmicroservice.utils.PrincipalAndSubordinateWireMockSetup

trait SubscriptionFieldsMock extends FieldsFixtures with FieldNameFixtures {
  self: PrincipalAndSubordinateWireMockSetup => // To allow for stubFor to work with environment

  def mockUpsert(env: Environment): Unit = {
    stubFor(env)(put(urlMatching(""".*/field/application/.*"""))
      .withRequestBody(matchingJsonPath("$.fields"))
      .willReturn(
        aResponse()
          .withStatus(OK)
      ))
  }

  def mockUpsertNotFound(env: Environment): Unit = {
    stubFor(env)(put(urlMatching(""".*/field/application/.*/context/.*/version/.*+"""))
      .withRequestBody(matchingJsonPath("$.fields"))
      .willReturn(
        aResponse()
          .withStatus(NOT_FOUND)
      ))
  }

  def mockUpsertWithBadRequest(env: Environment): Unit = {
    val errs: FieldErrorMap = Map(fieldNameOne -> "Error1")
    stubFor(env)(put(urlMatching(""".*/field/application/.*/context/.*/version/.*+"""))
      .withRequestBody(matchingJsonPath("$.fields"))
      .willReturn(
        aResponse()
          .withBody(Json.toJson(errs).toString())
          .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
          .withStatus(BAD_REQUEST)
      ))
  }

}
