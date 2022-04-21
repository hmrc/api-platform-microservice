/*
 * Copyright 2022 HM Revenue & Customs
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
import uk.gov.hmrc.apiplatformmicroservice.common.Recoveries
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.domain._
import uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.connectors.domain.BoxResponse
import uk.gov.hmrc.apiplatformmicroservice.pushpullnotifications.connectors.EnvironmentAwarePushPullNotificationsConnector
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.Environment

@Singleton
class BoxFetcher @Inject() (pushpullnotificationsConnector: EnvironmentAwarePushPullNotificationsConnector)(implicit ec: ExecutionContext)
    extends Recoveries {

  // TODO: Test me
  def fetchAllBoxes()(implicit hc: HeaderCarrier): Future[List[Box]] = {

    def toBox(environment: Environment)(box: BoxResponse) : Box = {
      Box(box.boxId, box.boxCreator, box.applicationId, box.subscriber, environment)
    }

    for {
      principalBoxes <- pushpullnotificationsConnector.principal.fetchAllBoxes()
      subordinateBoxes <- pushpullnotificationsConnector.principal.fetchAllBoxes()
      principalBoxesWithEnvironment = principalBoxes.map(toBox(Environment.PRODUCTION))
      subordinateBoxesWithEnvironment = subordinateBoxes.map(toBox(Environment.SANDBOX))
    } yield (principalBoxesWithEnvironment ++ subordinateBoxesWithEnvironment)
  }
}
