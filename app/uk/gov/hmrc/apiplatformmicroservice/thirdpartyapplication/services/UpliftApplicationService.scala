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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import cats.instances.future.catsStdInstancesForFuture

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.common.services.EitherTHelper
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.services.{ApiIdentifiersForUpliftFetcher, CdsVersionHandler}
import uk.gov.hmrc.apiplatformmicroservice.common.ApplicationLogger
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.Environment
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.PrincipalThirdPartyApplicationConnector
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.controllers.domain.UpliftRequest
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.{Application, CreateApplicationRequestV1, CreateApplicationRequestV2}

object UpliftApplicationService {
  type BadRequestMessage = String
}

@Singleton
class UpliftApplicationService @Inject() (
    apiIdentifiersForUpliftFetcher: ApiIdentifiersForUpliftFetcher,
    principalTPAConnector: PrincipalThirdPartyApplicationConnector,
    applicationByIdFetcher: ApplicationByIdFetcher,
    subscriptionService: SubscriptionService
  )(implicit val ec: ExecutionContext
  ) extends ApplicationLogger with EitherTHelper[String] {

  import UpliftApplicationService.BadRequestMessage

  private def subscribeAll(application: Application, apis: Set[ApiIdentifier])(implicit hc: HeaderCarrier): Future[SubscriptionService.CreateSubscriptionResult] = {
    subscriptionService.createManySubscriptionsForApplication(application, apis)
  }

  /*
   *   Params:
   *     app - the sandbox application in all it's glory
   *     appApiSubs - the subscriptions that the app has (some might be test support or example apis or ones that cannot be uplifted)
   *     requestedApiSubs - the subscriptions that were selected to be uplifted to production.
   *
   *   Returns:
   *     Left(msg) - for bad requests see msg
   */

  def upliftApplicationV2(
      app: Application,
      appApiSubs: Set[ApiIdentifier],
      upliftRequest: UpliftRequest
    )(implicit hc: HeaderCarrier
    ): Future[Either[BadRequestMessage, ApplicationId]] = {
    val requestedApiSubs: Set[ApiIdentifier] = upliftRequest.subscriptions
    val allRequestedSubsAreInAppSubs         = requestedApiSubs.intersect(appApiSubs) == requestedApiSubs
    (
      for {
        _                       <- cond(requestedApiSubs.nonEmpty, (), "Request contains no apis for uplifting the sandbox application")
        _                       <- cond(app.deployedTo.isSandbox, (), "Request cannot uplift production application")
        _                       <- cond(allRequestedSubsAreInAppSubs, (), "Request contains apis not found for the sandbox application")
        upliftableApis          <- liftF(apiIdentifiersForUpliftFetcher.fetch)
        remappedRequestSubs      = CdsVersionHandler.adjustSpecialCaseVersions(requestedApiSubs)
        filteredSubs             = remappedRequestSubs.filter(upliftableApis.contains)
        _                       <- cond(filteredSubs.nonEmpty, (), "Request contains apis that cannot be uplifted")
        filteredUpliftRequest    = upliftRequest.copy(subscriptions = filteredSubs)
        createApplicationRequest = CreateApplicationRequestV2(
                                     app.name,
                                     app.access,
                                     app.description,
                                     Environment.PRODUCTION,
                                     app.collaborators,
                                     filteredUpliftRequest,
                                     filteredUpliftRequest.requestedBy,
                                     app.id
                                   )
        newAppId                <- liftF(principalTPAConnector.createApplicationV2(createApplicationRequest))
        app                     <- fromOptionF(applicationByIdFetcher.fetchApplication(newAppId), "Amazingly no such app???")
        _                       <- liftF(subscribeAll(app, filteredSubs))
      } yield newAppId
    ).value
  }

  def upliftApplicationV1(
      app: Application,
      appApiSubs: Set[ApiIdentifier],
      requestedApiSubs: Set[ApiIdentifier]
    )(implicit hc: HeaderCarrier
    ): Future[Either[BadRequestMessage, ApplicationId]] = {
    val allRequestedSubsAreInAppSubs = requestedApiSubs.intersect(appApiSubs) == requestedApiSubs
    (
      for {
        _                       <- cond(requestedApiSubs.nonEmpty, (), "Request contains no apis for uplifting the sandbox application")
        _                       <- cond(app.deployedTo.isSandbox, (), "Request cannot uplift production application")
        _                       <- cond(allRequestedSubsAreInAppSubs, (), "Request contains apis not found for the sandbox application")
        upliftableApis          <- liftF(apiIdentifiersForUpliftFetcher.fetch)
        remappedRequestSubs      = CdsVersionHandler.adjustSpecialCaseVersions(requestedApiSubs)
        filteredSubs             = remappedRequestSubs.filter(upliftableApis.contains)
        _                       <- cond(filteredSubs.nonEmpty, (), "Request contains apis that cannot be uplifted")
        createApplicationRequest = CreateApplicationRequestV1(
                                     app.name,
                                     app.access,
                                     app.description,
                                     Environment.PRODUCTION,
                                     app.collaborators,
                                     None
                                   )
        newAppId                <- liftF(principalTPAConnector.createApplicationV1(createApplicationRequest))
        app                     <- fromOptionF(applicationByIdFetcher.fetchApplication(newAppId), "Amazingly no such app???")
        _                       <- liftF(subscribeAll(app, filteredSubs))
      } yield newAppId
    ).value
  }

  def fetchUpliftableApisForApplication(subscriptions: Set[ApiIdentifier])(implicit hc: HeaderCarrier): Future[Set[ApiIdentifier]] = {
    for {
      upliftableApis <- apiIdentifiersForUpliftFetcher.fetch
      filteredSubs    = subscriptions.filter(upliftableApis.contains)
    } yield filteredSubs
  }
}
