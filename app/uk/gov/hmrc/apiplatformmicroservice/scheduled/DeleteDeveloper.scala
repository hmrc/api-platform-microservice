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

package uk.gov.hmrc.apiplatformmicroservice.scheduled

import uk.gov.hmrc.apiplatformmicroservice.connectors.{ProductionThirdPartyApplicationConnector, SandboxThirdPartyApplicationConnector}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.Future.sequence

trait DeleteDeveloper {

  val sandboxApplicationConnector: SandboxThirdPartyApplicationConnector
  val productionApplicationConnector: ProductionThirdPartyApplicationConnector
  val deleteFunction: (String) => Future[Int]

  def deleteDeveloper(developerEmail: String)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[String] = {
    for {
      sandboxAppIds <- sandboxApplicationConnector.fetchApplicationsByEmail(developerEmail)
      productionAppIds <- productionApplicationConnector.fetchApplicationsByEmail(developerEmail)
      _ <- sequence(sandboxAppIds.map(sandboxApplicationConnector.removeCollaborator(_, developerEmail)))
      _ <- sequence(productionAppIds.map(productionApplicationConnector.removeCollaborator(_, developerEmail)))
      _ <- deleteFunction(developerEmail)
    } yield developerEmail
  }
}
