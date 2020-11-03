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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors

import javax.inject.{Inject, Singleton}
import play.api.http.ContentTypes._
import play.api.http.HeaderNames._
import play.api.libs.json.Json
import uk.gov.hmrc.apiplatformmicroservice.common.domain.services.CommonJsonFormatters
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.domain.{GetOrCreateUserIdRequest, GetOrCreateUserIdResponse, UnregisteredUserCreationRequest, UnregisteredUserResponse, UserResponse}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.domain.UnregisteredUserResponse._
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

private[thirdpartyapplication] object ThirdPartyDeveloperConnector extends CommonJsonFormatters {

  class ApplicationNotFound extends RuntimeException

  case class Config(
      applicationBaseUrl: String,
      jsonEncryptionKey: String)
}

@Singleton
private[thirdpartyapplication] class ThirdPartyDeveloperConnector @Inject() (
           val config: ThirdPartyDeveloperConnector.Config,
           http: HttpClient,
           encryptedJson: EncryptedJson) (implicit val ec: ExecutionContext) {

  lazy val serviceBaseUrl: String = config.applicationBaseUrl
  lazy val jsonEncryptionKey: String = config.jsonEncryptionKey

  def fetchByEmails(emails: Set[String])(implicit hc: HeaderCarrier): Future[Seq[UserResponse]] = {
    http.GET[Seq[UserResponse]](s"$serviceBaseUrl/developers", Seq("emails" -> emails.mkString(",")))
  }

  def getOrCreateUserId(getOrCreateUserIdRequest: GetOrCreateUserIdRequest)(implicit hc: HeaderCarrier): Future[GetOrCreateUserIdResponse] = {
      http.POST[GetOrCreateUserIdRequest, GetOrCreateUserIdResponse](s"$serviceBaseUrl/developers/user-id", getOrCreateUserIdRequest, Seq(CONTENT_TYPE -> JSON))
  }

  def fetchDeveloper(email: String)(implicit hc: HeaderCarrier): Future[Option[UserResponse]] = {
    http.GET[Option[UserResponse]](s"$serviceBaseUrl/developer", Seq("email" -> email)) recover {
      case _: NotFoundException => None
    }
  }

  def createUnregisteredUser(email: String)(implicit hc: HeaderCarrier): Future[UnregisteredUserResponse] = {
    encryptedJson.secretRequestJson[UnregisteredUserResponse](
      Json.toJson(UnregisteredUserCreationRequest(email)), { secretRequestJson =>
        http.POST(s"$serviceBaseUrl/unregistered-developer", secretRequestJson, Seq(CONTENT_TYPE -> JSON)) map { result =>
          result.json.as[UnregisteredUserResponse]
        }
      })
  }
}
