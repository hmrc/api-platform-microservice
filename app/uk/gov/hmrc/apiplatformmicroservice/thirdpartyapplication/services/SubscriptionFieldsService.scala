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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

import play.api.Logger
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.ApiIdentifier
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications._
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.subscriptions.SubscriptionFieldsDomain._
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.SubscriptionFieldsService.DefinitionsByApiVersion
import scala.util.{Failure, Success}

@Singleton
class SubscriptionFieldsService @Inject() (
    supplier: EnvironmentAwareConnectorsSupplier
  )(implicit val ec: ExecutionContext) {

  def logAndReturn[T](tag: String)(f: => Future[T]): Future[T] = {
    val v = f
    v.onComplete {
      case Success(s) => Logger.info(s"****$tag : Success " + s)
      case Failure(f) => Logger.info(s"****$tag : Failed " + f)
    }
    v
  }

  def fetchFieldsValues(
      application: Application,
      fieldDefinitions: Seq[SubscriptionFieldDefinition],
      apiIdentifier: ApiIdentifier
    )(implicit hc: HeaderCarrier
    ): Future[Seq[SubscriptionFieldValue]] = {
    val connector = supplier.forEnvironment(application.deployedTo).apiSubscriptionFieldsConnector

    if (fieldDefinitions.isEmpty) {
      Future.successful(Seq.empty[SubscriptionFieldValue])
    } else {
      logAndReturn("fetchFieldValues")(connector.fetchFieldValues(application.clientId, apiIdentifier))
    }
  }

  def getAllFieldDefinitions(environment: Environment)(implicit hc: HeaderCarrier): Future[DefinitionsByApiVersion] = {
    supplier
      .forEnvironment(environment)
      .apiSubscriptionFieldsConnector.fetchAllFieldDefinitions()
  }

  // def apisWithSubscriptions(application: Application)(implicit hc: HeaderCarrier): Future[Seq[APISubscriptionStatus]] = {

  //   def mergeAllSubscriptionVersionsWithDefinitions(api: APISubscription, fieldDefinitions: DefinitionsByApiVersion): Seq[Future[APISubscriptionStatus]] = {
  //     def mergeSubscriptionVersionWithDefinition(api: APISubscription, version: VersionSubscription, fieldDefinitions: DefinitionsByApiVersion): Future[APISubscriptionStatus] = {

  //       // Logger.info("*****ApiSub "+api)
  //       // Logger.info("*****VerSub "+version)
  //       val apiIdentifier = ApiIdentifier(api.context, version.version.version)

  //       val subscriptionFieldsWithoutValues: Seq[SubscriptionFieldDefinition] =
  //         fieldDefinitions.getOrElse(apiIdentifier, Seq.empty)

  //       val subscriptionFieldsWithValues: Future[Seq[SubscriptionFieldValue]] =
  //         fetchFieldsValues(application, subscriptionFieldsWithoutValues, apiIdentifier)

  //       logAndReturn("merge")(subscriptionFieldsWithValues.map { fields: Seq[SubscriptionFieldValue] =>
  //         val wrapper = SubscriptionFieldsWrapper(application.id, application.clientId, api.context, version.version.version, fields)
  //         APISubscriptionStatus(api.name, api.serviceName, api.context, version.version, version.subscribed, api.requiresTrust.getOrElse(false), wrapper, api.isTestSupport)
  //       })
  //     }

  //     def versionSorter(v1: APIVersion, v2: APIVersion) = {
  //       val nonNumericOrPeriodRegex = "[^\\d^.]*"
  //       val fallback = Array(1, 0, 0)

  //       val v1Parts = Try(v1.version.replaceAll(nonNumericOrPeriodRegex, "").split("\\.").map(_.toInt)).getOrElse(fallback)
  //       val v2Parts = Try(v2.version.replaceAll(nonNumericOrPeriodRegex, "").split("\\.").map(_.toInt)).getOrElse(fallback)
  //       val pairs = v1Parts.zip(v2Parts)

  //       val firstUnequalPair = pairs.find { case (one, two) => one != two }
  //       firstUnequalPair.fold(v1.version.length > v2.version.length) { case (a, b) => a > b }
  //     }

  //     def descendingVersion(v1: VersionSubscription, v2: VersionSubscription) = {
  //       versionSorter(v1.version, v2.version)
  //     }

  //     api.versions
  //       .filterNot(_.version.status == RETIRED)
  //       .filterNot(s => s.version.status == DEPRECATED && !s.subscribed)
  //       .sortWith(descendingVersion)
  //       .map(mergeSubscriptionVersionWithDefinition(api, _, fieldDefinitions))
  //   }

  //   val thirdPartyAppConnector = supplier.forEnvironment(application.deployedTo).thirdPartyApplicationConnector

  //   for {
  //     fieldDefinitions: DefinitionsByApiVersion <- getAllFieldDefinitions(application.deployedTo)
  //     subscriptions: Seq[APISubscription] <- thirdPartyAppConnector.fetchSubscriptions(application.id)
  //     apiVersions <- logAndReturn("ALL ")(Future.sequence(subscriptions.flatMap(mergeAllSubscriptionVersionsWithDefinitions(_, fieldDefinitions))))
  //   } yield apiVersions
  // }

}

object SubscriptionFieldsService {

  trait SubscriptionFieldsConnector {
    def fetchFieldValues(clientId: ClientId, apiIdentifier: ApiIdentifier)(implicit hc: HeaderCarrier): Future[Seq[SubscriptionFieldValue]]

    def fetchAllFieldDefinitions()(implicit hc: HeaderCarrier): Future[DefinitionsByApiVersion]
  }

  type DefinitionsByApiVersion = Map[ApiIdentifier, Seq[SubscriptionFieldDefinition]]

  object DefinitionsByApiVersion {
    val empty = Map.empty[ApiIdentifier, Seq[SubscriptionFieldDefinition]]
  }
}
