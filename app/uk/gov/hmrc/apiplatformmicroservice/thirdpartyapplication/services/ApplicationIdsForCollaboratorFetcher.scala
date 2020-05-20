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
import play.api.Logger
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.{ProductionThirdPartyApplicationConnector, SandboxThirdPartyApplicationConnector}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class ApplicationIdsForCollaboratorFetcher @Inject()(sandboxTpaConnector: SandboxThirdPartyApplicationConnector,
                                                     productionTpaConnector: ProductionThirdPartyApplicationConnector)
                                                    (implicit ec: ExecutionContext) {

  def apply(email: String)(implicit hc: HeaderCarrier): Future[Seq[String]] = {
    val sandboxAppIds = sandboxTpaConnector.fetchApplicationsByEmail(email) recover recovery
    val prodAppIds = productionTpaConnector.fetchApplicationsByEmail(email)

    for {
      sandbox <- sandboxAppIds
      production <- prodAppIds
    } yield sandbox ++ production
  }

  private def recovery: PartialFunction[Throwable, Seq[String]] = {
    case NonFatal(e) =>
      Logger.error(s"Error occurred: ${e.getMessage}", e)
      Seq.empty[String]
  }

}
