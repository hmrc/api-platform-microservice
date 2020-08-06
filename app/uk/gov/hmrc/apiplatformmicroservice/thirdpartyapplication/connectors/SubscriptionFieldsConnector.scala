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

import java.net.URLEncoder.encode

import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.http.HttpReads.Implicits._

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.apiplatformmicroservice.common.ProxiedHttpClient
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.SubscriptionFieldsService._
import uk.gov.hmrc.http.HeaderCarrier
import akka.pattern.FutureTimeoutSupport
import akka.actor.ActorSystem
import com.google.inject.{Inject, Singleton}
import uk.gov.hmrc.http.UpstreamErrorResponse.WithStatusCode
import play.api.http.Status
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.Environment
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.ApiIdentifier
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.ClientId

abstract class AbstractSubscriptionFieldsConnector(implicit ec: ExecutionContext) extends SubscriptionFieldsConnector with FieldDefinitionFormatters {
  protected val httpClient: HttpClient
  protected val proxiedHttpClient: ProxiedHttpClient
  val environment: Environment
  val serviceBaseUrl: String

  import SubscriptionFieldsConnectorDomain._
  import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.subscriptions.SubscriptionFieldsDomain._

  def http: HttpClient

  def fetchFieldValues(
      clientId: ClientId,
      apiIdentifier: ApiIdentifier
    )(implicit hc: HeaderCarrier
    ): Future[Seq[SubscriptionFieldValue]] = {

    def getDefinitions() = fetchFieldDefinitions(apiIdentifier)

    internalFetchFieldValues(getDefinitions)(clientId, apiIdentifier)
  }

  def fetchAllFieldDefinitions()(implicit hc: HeaderCarrier): Future[DefinitionsByApiVersion] = {
    val url = s"$serviceBaseUrl/definition"
    for {
      response <- http.GET[AllApiFieldDefinitions](url)
    } yield toDomain(response)
  }

  def fetchFieldDefinitions(apiIdentifier: ApiIdentifier)(implicit hc: HeaderCarrier): Future[Seq[SubscriptionFieldDefinition]] = {
    val url = urlSubscriptionFieldDefinition(apiIdentifier)
    http.GET[ApiFieldDefinitions](url).map(response => response.fieldDefinitions.map(toDomain)) recover recovery(Seq.empty)
  }

  private def internalFetchFieldValues(
      getDefinitions: () => Future[Seq[SubscriptionFieldDefinition]]
    )(clientId: ClientId,
      apiIdentifier: ApiIdentifier
    )(implicit hc: HeaderCarrier
    ): Future[Seq[SubscriptionFieldValue]] = {

    def joinFieldValuesToDefinitions(
        defs: Seq[SubscriptionFieldDefinition],
        fieldValues: Fields
      ): Seq[SubscriptionFieldValue] = {
      defs.map(field => SubscriptionFieldValue(field, fieldValues.getOrElse(field.name, "")))
    }

    def ifDefinitionsGetValues(
        definitions: Seq[SubscriptionFieldDefinition]
      ): Future[Option[ApplicationApiFieldValues]] = {
      if (definitions.isEmpty) {
        Future.successful(None)
      } else {
        fetchApplicationApiValues(clientId, apiIdentifier)
      }
    }

    for {
      definitions: Seq[SubscriptionFieldDefinition] <- getDefinitions()
      subscriptionFields <- ifDefinitionsGetValues(definitions)
      fieldValues = subscriptionFields.fold(Fields.empty)(_.fields)
    } yield joinFieldValuesToDefinitions(definitions, fieldValues)
  }

  private def fetchApplicationApiValues(clientId: ClientId, apiIdentifier: ApiIdentifier)(implicit hc: HeaderCarrier): Future[Option[ApplicationApiFieldValues]] = {
    val url = urlSubscriptionFieldValues(clientId, apiIdentifier)
    http.GET[Option[ApplicationApiFieldValues]](url) recover recovery(None)
  }

  private def urlEncode(str: String, encoding: String = "UTF-8") = encode(str, encoding)

  private def urlSubscriptionFieldDefinition(apiIdentifier: ApiIdentifier) =
    s"$serviceBaseUrl/definition/context/${urlEncode(apiIdentifier.context)}/version/${urlEncode(apiIdentifier.version)}"

  private def urlSubscriptionFieldValues(clientId: ClientId, apiIdentifier: ApiIdentifier) =
    s"$serviceBaseUrl/field/application/${urlEncode(clientId.value)}/context/${urlEncode(apiIdentifier.context)}/version/${urlEncode(apiIdentifier.version)}"

  private def recovery[T](defaultValue: T): PartialFunction[Throwable, T] = {
    case WithStatusCode(Status.NOT_FOUND, _) => defaultValue
  }
}

object SubordinateSubscriptionFieldsConnector {
  case class Config(serviceBaseUrl: String, useProxy: Boolean, bearerToken: String, apiKey: String)
}

@Singleton
class SubordinateSubscriptionFieldsConnector @Inject() (
    val appConfig: SubordinateSubscriptionFieldsConnector.Config,
    val httpClient: HttpClient,
    val proxiedHttpClient: ProxiedHttpClient,
    val actorSystem: ActorSystem,
    val futureTimeout: FutureTimeoutSupport
  )(implicit val ec: ExecutionContext)
    extends AbstractSubscriptionFieldsConnector {

  val environment: Environment = Environment.SANDBOX
  val serviceBaseUrl: String = appConfig.serviceBaseUrl
  val useProxy: Boolean = appConfig.useProxy
  val bearerToken: String = appConfig.bearerToken
  val apiKey: String = appConfig.apiKey

  def http: HttpClient = if (useProxy) proxiedHttpClient.withHeaders(bearerToken, apiKey) else httpClient
}

object PrincipalSubscriptionFieldsConnector {
  case class Config(serviceBaseUrl: String)
}

@Singleton
class PrincipalSubscriptionFieldsConnector @Inject() (
    val appConfig: PrincipalSubscriptionFieldsConnector.Config,
    val httpClient: HttpClient,
    val proxiedHttpClient: ProxiedHttpClient,
    val actorSystem: ActorSystem,
    val futureTimeout: FutureTimeoutSupport
  )(implicit val ec: ExecutionContext)
    extends AbstractSubscriptionFieldsConnector {

  def http: HttpClient = httpClient

  val environment: Environment = Environment.PRODUCTION
  val serviceBaseUrl: String = appConfig.serviceBaseUrl
}
