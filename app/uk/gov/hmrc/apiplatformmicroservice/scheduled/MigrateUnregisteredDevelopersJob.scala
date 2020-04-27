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

import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import javax.inject.Inject
import org.joda.time.Duration
import play.api.Logger
import play.modules.reactivemongo.ReactiveMongoComponent
import uk.gov.hmrc.apiplatformmicroservice.connectors.{ProductionThirdPartyApplicationConnector, SandboxThirdPartyApplicationConnector, ThirdPartyDeveloperConnector}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lock.{LockKeeper, LockRepository}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class MigrateUnregisteredDevelopersJob @Inject()(override val lockKeeper: MigrateUnregisteredDevelopersJobLockKeeper,
                                                jobConfig: MigrateUnregisteredDevelopersJobConfig,
                                                developerConnector: ThirdPartyDeveloperConnector,
                                                sandboxApplicationConnector: SandboxThirdPartyApplicationConnector,
                                                productionApplicationConnector: ProductionThirdPartyApplicationConnector)
                                                (implicit val mat: Materializer)
  extends ScheduledMongoJob {

  override def name: String = "MigrateUnregisteredDevelopersJob"

  override def interval: FiniteDuration = jobConfig.interval
  override def initialDelay: FiniteDuration = jobConfig.initialDelay
  implicit val hc: HeaderCarrier = HeaderCarrier()

  override def runJob(implicit ec: ExecutionContext): Future[RunningOfJobSuccessful] = {
    Logger.info("Starting MigrateUnregisteredDevelopersJob")
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val parallelism = 10
    val unregisteredUsersSink = Sink.foreachAsync[String](parallelism) { email =>
      developerConnector.createUnregisteredDeveloper(email).map(_ => ())
    }

    (for {
      sandboxCollaborators <- sandboxApplicationConnector.fetchAllCollaborators
      _ = Logger.info(s"Found ${sandboxCollaborators.size} sandbox collaborators")
      prodCollaborators <- productionApplicationConnector.fetchAllCollaborators
      _ = Logger.info(s"Found ${prodCollaborators.size} production collaborators")
      sourceOfCollaborators = Source.fromIterator(() => (sandboxCollaborators ++ prodCollaborators).toIterator)
      _ <- sourceOfCollaborators.runWith(unregisteredUsersSink)
    } yield RunningOfJobSuccessful) recoverWith {
      case NonFatal(e) =>
        Logger.error("Could not migrate unregistered developers", e)
        Future.failed(RunningOfJobFailed(name, e))
    }
  }
}

class MigrateUnregisteredDevelopersJobLockKeeper @Inject()(mongo: ReactiveMongoComponent) extends LockKeeper {
  override def repo: LockRepository = new LockRepository()(mongo.mongoConnector.db)

  override def lockId: String = "MigrateUnregisteredDevelopersJob"

  override val forceLockReleaseAfter: Duration = Duration.standardHours(2)
}

case class MigrateUnregisteredDevelopersJobConfig(initialDelay: FiniteDuration, interval: FiniteDuration, enabled: Boolean)
