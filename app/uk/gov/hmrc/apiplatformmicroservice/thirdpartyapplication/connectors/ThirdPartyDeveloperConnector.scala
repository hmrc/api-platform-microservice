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

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress
import uk.gov.hmrc.apiplatform.modules.tpd.core.domain.models.User
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.domain._

private[thirdpartyapplication] object ThirdPartyDeveloperConnector {

  class ApplicationNotFound extends RuntimeException

  case class Config(
      applicationBaseUrl: String
    )
}

@Singleton
private[thirdpartyapplication] class ThirdPartyDeveloperConnector @Inject() (
    val config: ThirdPartyDeveloperConnector.Config,
    http: HttpClientV2
  )(implicit val ec: ExecutionContext
  ) {

  lazy val serviceBaseUrl: String = config.applicationBaseUrl

  def fetchByEmails(emails: Set[LaxEmailAddress])(implicit hc: HeaderCarrier): Future[Seq[User]] = {
    http.post(url"$serviceBaseUrl/developers/get-by-emails")
      .withBody(Json.toJson(emails.toList))
      .execute[Seq[User]]

  }

  def getOrCreateUserId(getOrCreateUserIdRequest: GetOrCreateUserIdRequest)(implicit hc: HeaderCarrier): Future[GetOrCreateUserIdResponse] = {
    http.post(url"$serviceBaseUrl/developers/user-id")
      .withBody(Json.toJson(getOrCreateUserIdRequest))
      .execute[GetOrCreateUserIdResponse]
  }
}
