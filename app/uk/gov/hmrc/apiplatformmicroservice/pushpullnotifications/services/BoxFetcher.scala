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

package uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.services

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatformmicroservice.common.Recoveries
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.Environment
import uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.connectors.EnvironmentAwarePushPullNotificationsConnector
import uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.domain._

@Singleton
class BoxFetcher @Inject() (pushpullnotificationsConnector: EnvironmentAwarePushPullNotificationsConnector)(implicit ec: ExecutionContext)
    extends Recoveries {

  def fetchAllBoxes()(implicit hc: HeaderCarrier): Future[List[Box]] = {
    for {
      subordinateBoxes               <- pushpullnotificationsConnector.subordinate.fetchAllBoxes()
      principalBoxes                 <- pushpullnotificationsConnector.principal.fetchAllBoxes()
      subordinateBoxesWithEnvironment = subordinateBoxes.map(_.toBox(Environment.SANDBOX))
      principalBoxesWithEnvironment   = principalBoxes.map(_.toBox(Environment.PRODUCTION))

    } yield (subordinateBoxesWithEnvironment ++ principalBoxesWithEnvironment)
  }
}
