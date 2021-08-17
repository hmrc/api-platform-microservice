/*
 * Copyright 2021 HM Revenue & Customs
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
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.CreateApplicationRequest

import scala.concurrent.ExecutionContext
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.services.ApiIdentifiersForUpliftFetcher
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.PrincipalThirdPartyApplicationConnector
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiIdentifier
import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.Environment
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.Application
import play.api.Logger
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.services.CdsVersionHandler

object UpliftApplicationService {
  type BadRequestMessage = String
}

@Singleton
class UpliftApplicationService @Inject() (
    val apiIdentifiersForUpliftFetcher: ApiIdentifiersForUpliftFetcher,
    val principalTPAConnector: PrincipalThirdPartyApplicationConnector
  )(implicit val ec: ExecutionContext) {

  import UpliftApplicationService.BadRequestMessage

  private def createAppIfItHasAnySubs(app: Application, filteredSubs: Set[ApiIdentifier])(implicit hc: HeaderCarrier): Future[Either[BadRequestMessage, ApplicationId]] = {
    if(filteredSubs.isEmpty) {
      val message = s"No subscriptions for uplift of application with id: ${app.id.value}"
      Logger.info(message)
      successful(Left(message))
    }
    else {
      val createApplicationRequest =  CreateApplicationRequest(
                                        app.name,
                                        app.access,
                                        app.description,
                                        Environment.PRODUCTION,
                                        app.collaborators,
                                        filteredSubs
                                      )
      principalTPAConnector.createApplication(createApplicationRequest).map(Right(_))
    }
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

  def upliftApplication(app: Application, appApiSubs: Set[ApiIdentifier], requestedApiSubs: Set[ApiIdentifier])(implicit hc: HeaderCarrier): Future[Either[BadRequestMessage,ApplicationId]] = {
    require(requestedApiSubs.nonEmpty)
    
    if(app.deployedTo.isProduction) {
      successful(Left("Request cannot uplift production application"))
    }
    else if(requestedApiSubs.intersect(appApiSubs) != requestedApiSubs) {
      successful(Left("Request contains apis not found for the sandbox application"))
    }
    else {
      for {
        upliftableApis      <- apiIdentifiersForUpliftFetcher.fetch
        remappedRequestSubs = CdsVersionHandler.adjustSpecialCaseVersions(requestedApiSubs)
        filteredSubs        = remappedRequestSubs.filter(upliftableApis.contains)
        newAppId           <- createAppIfItHasAnySubs(app, filteredSubs)
      } yield newAppId
    }
  }
}
