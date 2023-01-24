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

import play.api.http.ContentTypes._
import play.api.http.HeaderNames._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HttpClient, _}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.domain._

private[thirdpartyapplication] object ThirdPartyDeveloperConnector {

  class ApplicationNotFound extends RuntimeException

  case class Config(
      applicationBaseUrl: String,
      jsonEncryptionKey: String
    )
}

@Singleton
private[thirdpartyapplication] class ThirdPartyDeveloperConnector @Inject() (
    val config: ThirdPartyDeveloperConnector.Config,
    http: HttpClient,
    encryptedJson: EncryptedJson
  )(implicit val ec: ExecutionContext
  ) {

  lazy val serviceBaseUrl: String    = config.applicationBaseUrl
  lazy val jsonEncryptionKey: String = config.jsonEncryptionKey

  def fetchByEmails(emails: Set[LaxEmailAddress])(implicit hc: HeaderCarrier): Future[Seq[UserResponse]] = {
    http.POST[List[String], Seq[UserResponse]](s"$serviceBaseUrl/developers/get-by-emails", emails.toList.map(_.value))
  }

  def getOrCreateUserId(getOrCreateUserIdRequest: GetOrCreateUserIdRequest)(implicit hc: HeaderCarrier): Future[GetOrCreateUserIdResponse] = {
    http.POST[GetOrCreateUserIdRequest, GetOrCreateUserIdResponse](s"$serviceBaseUrl/developers/user-id", getOrCreateUserIdRequest, Seq(CONTENT_TYPE -> JSON))
  }
}
