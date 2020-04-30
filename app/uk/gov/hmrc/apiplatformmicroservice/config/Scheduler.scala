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

package uk.gov.hmrc.apiplatformmicroservice.config

import com.google.inject.AbstractModule
import javax.inject.{Inject, Singleton}
import play.api.Application
import play.api.inject.ApplicationLifecycle
import uk.gov.hmrc.apiplatformmicroservice.scheduled.{DeleteUnregisteredDevelopersJob, DeleteUnregisteredDevelopersJobConfig, DeleteUnverifiedDevelopersJob, DeleteUnverifiedDevelopersJobConfig, UpdateUnusedApplicationRecordsJob, UpdateUnusedProductionApplicationRecordJob, UpdateUnusedSandboxApplicationRecordJob}
import uk.gov.hmrc.play.scheduling.{RunningOfScheduledJobs, ScheduledJob}

import scala.concurrent.ExecutionContext

class SchedulerModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[Scheduler]).asEagerSingleton()
  }
}

@Singleton
class Scheduler @Inject()(deleteUnverifiedDevelopersJobConfig: DeleteUnverifiedDevelopersJobConfig,
                          deleteUnverifiedDevelopersJob: DeleteUnverifiedDevelopersJob,
                          deleteUnregisteredDevelopersJobConfig: DeleteUnregisteredDevelopersJobConfig,
                          deleteUnregisteredDevelopersJob: DeleteUnregisteredDevelopersJob,
                          updateUnusedSandboxApplicationRecordsJob: UpdateUnusedSandboxApplicationRecordJob,
                          updateUnusedProductionApplicationRecordJob: UpdateUnusedProductionApplicationRecordJob,
                          override val applicationLifecycle: ApplicationLifecycle,
                          override val application: Application)
                         (implicit val ec: ExecutionContext) extends RunningOfScheduledJobs {
  lazy val deleteUnverifiedDevsJob: Seq[ScheduledJob] = if (deleteUnverifiedDevelopersJobConfig.enabled) {
    Seq(deleteUnverifiedDevelopersJob)
  } else {
    Seq.empty
  }

  lazy val deleteUnregisteredDevsJob: Seq[ScheduledJob] = if (deleteUnregisteredDevelopersJobConfig.enabled) {
    Seq(deleteUnregisteredDevelopersJob)
  } else {
    Seq.empty
  }

  lazy val unusedApplicationJobs: Seq[ScheduledJob] =
    Seq(updateUnusedSandboxApplicationRecordsJob, updateUnusedProductionApplicationRecordJob)
      .filter(_.isEnabled)

  override lazy val scheduledJobs: Seq[ScheduledJob] = deleteUnverifiedDevsJob ++ deleteUnregisteredDevsJob ++ unusedApplicationJobs
}
