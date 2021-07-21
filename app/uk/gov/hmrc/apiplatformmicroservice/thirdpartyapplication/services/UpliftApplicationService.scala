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
import scala.concurrent.Future.failed
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.Environment
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.Application
import uk.gov.hmrc.http.BadRequestException
import play.api.Logger
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.services.CdsVersionHandler

@Singleton
class UpliftApplicationService @Inject() (
    val apiIdentifiersForUpliftFetcher: ApiIdentifiersForUpliftFetcher,
    val principalTPAConnector: PrincipalThirdPartyApplicationConnector
  )(implicit val ec: ExecutionContext) {

  private def createAppIfItHasAnySubs(filteredSubs: Set[ApiIdentifier], app: Application)(implicit hc: HeaderCarrier): Future[ApplicationId] = {
    if(filteredSubs.isEmpty) {
      val errorMessage = s"No subscriptions for uplift of application with id: ${app.id.value}"
      Logger.info(errorMessage)
      failed(new BadRequestException(errorMessage))
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
      principalTPAConnector.createApplication(createApplicationRequest)
    }
  }

  def upliftApplication(app: Application, subs: Set[ApiIdentifier])(implicit hc: HeaderCarrier): Future[ApplicationId] =
    for {
      upliftableApis <- apiIdentifiersForUpliftFetcher.fetch
      remappedSubs = CdsVersionHandler.adjustSpecialCaseVersions(subs) 
      filteredSubs   = remappedSubs.filter(sub => upliftableApis.contains(sub))
      newAppId <- createAppIfItHasAnySubs(filteredSubs, app)
    } yield newAppId
}
