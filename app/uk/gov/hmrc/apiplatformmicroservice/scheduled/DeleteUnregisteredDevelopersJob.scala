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

import javax.inject.Inject
import org.joda.time.Duration
import play.api.Logger
import play.modules.reactivemongo.ReactiveMongoComponent
import uk.gov.hmrc.apiplatformmicroservice.connectors.{ThirdPartyDeveloperConnector, ProductionThirdPartyApplicationConnector, SandboxThirdPartyApplicationConnector}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lock.{LockKeeper, LockRepository}

import scala.concurrent.Future.sequence
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class DeleteUnregisteredDevelopersJob @Inject()(override val lockKeeper: DeleteUnregisteredDevelopersJobLockKeeper,
                                                jobConfig: DeleteUnregisteredDevelopersJobConfig,
                                                developerConnector: ThirdPartyDeveloperConnector,
                                                override val sandboxApplicationConnector: SandboxThirdPartyApplicationConnector,
                                                override val productionApplicationConnector: ProductionThirdPartyApplicationConnector)
  extends ScheduledMongoJob with DeleteDeveloper {

  override def name: String = "DeleteUnregisteredDevelopersJob"

  override def interval: FiniteDuration = jobConfig.interval
  override def initialDelay: FiniteDuration = jobConfig.initialDelay
  implicit val hc: HeaderCarrier = HeaderCarrier()
  override val deleteFunction: (String) => Future[Int] = developerConnector.deleteUnregisteredDeveloper

  override def runJob(implicit ec: ExecutionContext): Future[RunningOfJobSuccessful] = {
    Logger.info("Starting DeleteUnregisteredDevelopersJob")
    implicit val hc: HeaderCarrier = HeaderCarrier()

    (for {
      developerEmails <- developerConnector.fetchExpiredUnregisteredDevelopers(jobConfig.limit)
      _ = Logger.info(s"Found ${developerEmails.size} unregistered developers")
      _ <- sequence(developerEmails.map(deleteDeveloper(_)))
    } yield RunningOfJobSuccessful) recoverWith {
      case NonFatal(e) =>
        Logger.error("Could not delete unregistered developers", e)
        Future.failed(RunningOfJobFailed(name, e))
    }
  }
}

class DeleteUnregisteredDevelopersJobLockKeeper @Inject()(mongo: ReactiveMongoComponent) extends LockKeeper {
  override def repo: LockRepository = new LockRepository()(mongo.mongoConnector.db)

  override def lockId: String = "DeleteUnregisteredDevelopersJob"

  override val forceLockReleaseAfter: Duration = Duration.standardHours(1)
}

case class DeleteUnregisteredDevelopersJobConfig(initialDelay: FiniteDuration, interval: FiniteDuration, enabled: Boolean, limit: Int)
