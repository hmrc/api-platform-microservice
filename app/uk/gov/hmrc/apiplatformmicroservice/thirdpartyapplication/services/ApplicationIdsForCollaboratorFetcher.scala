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
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.{ProductionThirdPartyApplicationConnector, SandboxThirdPartyApplicationConnector}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApplicationIdsForCollaboratorFetcher @Inject()(sandboxTpaConnector: SandboxThirdPartyApplicationConnector,
                                                     productionTpaConnector: ProductionThirdPartyApplicationConnector)
                                                    (implicit ec: ExecutionContext) {

  def apply(email: String)(implicit hc: HeaderCarrier): Future[Seq[String]] = {
    for {
      sandbox <- sandboxTpaConnector.fetchApplicationsByEmail(email)
      production <- productionTpaConnector.fetchApplicationsByEmail(email)
    } yield sandbox ++ production
  }
}
