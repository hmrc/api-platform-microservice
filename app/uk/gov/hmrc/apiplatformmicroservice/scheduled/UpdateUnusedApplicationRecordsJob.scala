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
import java.util.UUID

import javax.inject.Inject
import net.ceedubs.ficus.Ficus._
import org.joda.time.DateTime
import play.api.Configuration
import play.modules.reactivemongo.ReactiveMongoComponent
import uk.gov.hmrc.apiplatformmicroservice.connectors.ThirdPartyApplicationConnector
import uk.gov.hmrc.apiplatformmicroservice.models.Environment.Environment
import uk.gov.hmrc.apiplatformmicroservice.models.{ApplicationUsageDetails, Environment, UnusedApplication}
import uk.gov.hmrc.apiplatformmicroservice.repository.UnusedApplicationsRepository

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

abstract class UpdateUnusedApplicationRecordsJob @Inject()(environment: Environment,
                                                           thirdPartyApplicationConnector: ThirdPartyApplicationConnector,
                                                           unusedApplicationsRepository: UnusedApplicationsRepository,
                                                           configuration: Configuration,
                                                           mongo: ReactiveMongoComponent)
  extends TimedJob(s"UpdateUnusedApplicationsRecords-$environment", configuration, mongo) {

  val DeleteUnusedApplicationsAfter: FiniteDuration = configuration.underlying.as[FiniteDuration]("deleteUnusedApplicationsAfter")
  val NotifyDeletionPendingInAdvance: FiniteDuration = configuration.underlying.as[FiniteDuration]("notifyDeletionPendingInAdvance")

  def notificationCutoffDate(): DateTime =
    DateTime.now
      .minus(DeleteUnusedApplicationsAfter.toMillis)
      .plus(NotifyDeletionPendingInAdvance.toMillis)

  override def functionToExecute()(implicit executionContext: ExecutionContext): Future[RunningOfJobSuccessful] = {
    def unknownApplications(knownApplications: List[UnusedApplication], currentUnusedApplications: List[ApplicationUsageDetails]): Seq[UnusedApplication] = {
      val knownApplicationIds = knownApplications.map(_.applicationId)
      currentUnusedApplications
        .filterNot(app => knownApplicationIds.contains(app.applicationId))
        .map(app => UnusedApplication(app.applicationId, environment, app.lastAccessDate.getOrElse(app.creationDate)))
    }

    def noLongerUnusedApplications(knownApplications: List[UnusedApplication], currentUnusedApplications: List[ApplicationUsageDetails]): Set[UUID] = {
      val currentUnusedApplicationIds = currentUnusedApplications.map(_.applicationId)
      knownApplications
        .filterNot(app => currentUnusedApplicationIds.contains(app.applicationId))
        .map(_.applicationId)
        .toSet
    }

    def removeApplications(applicationsToRemove: Set[UUID]) =
      Future.sequence(applicationsToRemove.map(unusedApplicationsRepository.deleteApplication(environment, _)))

    for {
      knownApplications <- unusedApplicationsRepository.applicationsByEnvironment(environment)
      currentUnusedApplications <- thirdPartyApplicationConnector.applicationsLastUsedBefore(notificationCutoffDate())
      _ <- unusedApplicationsRepository.bulkInsert(unknownApplications(knownApplications, currentUnusedApplications))
      _ <- removeApplications(noLongerUnusedApplications(knownApplications, currentUnusedApplications))
    } yield RunningOfJobSuccessful
  }
}

class UpdateUnusedSandboxApplicationRecordJob @Inject()(thirdPartyApplicationConnector: ThirdPartyApplicationConnector,
                                                        unusedApplicationsRepository: UnusedApplicationsRepository,
                                                        configuration: Configuration,
                                                        mongo: ReactiveMongoComponent)
  extends UpdateUnusedApplicationRecordsJob(Environment.SANDBOX, thirdPartyApplicationConnector, unusedApplicationsRepository, configuration, mongo)

class UpdateUnusedProductionApplicationRecordJob @Inject()(thirdPartyApplicationConnector: ThirdPartyApplicationConnector,
                                                        unusedApplicationsRepository: UnusedApplicationsRepository,
                                                        configuration: Configuration,
                                                        mongo: ReactiveMongoComponent)
  extends UpdateUnusedApplicationRecordsJob(Environment.PRODUCTION, thirdPartyApplicationConnector, unusedApplicationsRepository, configuration, mongo)