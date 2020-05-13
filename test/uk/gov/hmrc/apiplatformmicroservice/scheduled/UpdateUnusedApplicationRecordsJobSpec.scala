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

import com.typesafe.config.{Config, ConfigFactory}
import org.joda.time.DateTime
import org.mockito.Mockito.{verifyNoInteractions, when, verify, times}
import org.mockito.{ArgumentCaptor, ArgumentMatchersSugar}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.commands.MultiBulkWriteResult
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.connectors.{ProductionThirdPartyApplicationConnector, SandboxThirdPartyApplicationConnector}
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.Environment.Environment
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.{ApplicationUsageDetails, Environment, UnusedApplication}
import uk.gov.hmrc.apiplatformmicroservice.repository.UnusedApplicationsRepository
import uk.gov.hmrc.mongo.{MongoConnector, MongoSpecSupport}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UpdateUnusedApplicationRecordsJobSpec extends PlaySpec
  with MockitoSugar with ArgumentMatchersSugar with MongoSpecSupport with FutureAwaits with DefaultAwaitTimeout {

  trait Setup {
    val environmentName = "Test Environment"

    def jobConfiguration(): Config = {
      ConfigFactory.parseString(
        s"""
           |deleteUnusedApplicationsAfter = 365d
           |notifyDeletionPendingInAdvance = 30d
           |
           |UpdateUnusedApplicationsRecords-SANDBOX {
           |  startTime = "00:30"
           |  executionInterval = 1d
           |  enabled = false
           |}
           |
           |UpdateUnusedApplicationsRecords-PRODUCTION {
           |  startTime = "01:00"
           |  executionInterval = 1d
           |  enabled = false
           |}
           |
           |""".stripMargin)
    }

    val mockSandboxThirdPartyApplicationConnector: SandboxThirdPartyApplicationConnector = mock[SandboxThirdPartyApplicationConnector]
    val mockProductionThirdPartyApplicationConnector: ProductionThirdPartyApplicationConnector = mock[ProductionThirdPartyApplicationConnector]
    val mockUnusedApplicationsRepository: UnusedApplicationsRepository = mock[UnusedApplicationsRepository]

    val reactiveMongoComponent: ReactiveMongoComponent = new ReactiveMongoComponent {
      override def mongoConnector: MongoConnector = mongoConnectorForTest
    }
  }

  trait SandboxJobSetup extends Setup {
    val configuration = new Configuration(jobConfiguration())

    val underTest = new UpdateUnusedSandboxApplicationRecordJob(
      mockSandboxThirdPartyApplicationConnector,
      mockUnusedApplicationsRepository,
      configuration,
      reactiveMongoComponent
    )
  }

  trait ProductionJobSetup extends Setup {
    val configuration = new Configuration(jobConfiguration())

    val underTest = new UpdateUnusedProductionApplicationRecordJob(
      mockProductionThirdPartyApplicationConnector,
      mockUnusedApplicationsRepository,
      configuration,
      reactiveMongoComponent
    )
  }

  "SANDBOX job" should {
    "add all newly discovered unused applications to database" in new SandboxJobSetup {
      val applicationWithLastUseDate: (ApplicationUsageDetails, UnusedApplication) =
        applicationDetails(Environment.SANDBOX, DateTime.now.minusMonths(13), Some(DateTime.now.minusMonths(13)))
      val applicationWithoutLastUseDate: (ApplicationUsageDetails, UnusedApplication) =
        applicationDetails(Environment.SANDBOX, DateTime.now.minusMonths(13), None)

      when(mockSandboxThirdPartyApplicationConnector.applicationsLastUsedBefore(*))
        .thenReturn(Future.successful(List(applicationWithLastUseDate._1, applicationWithoutLastUseDate._1)))
      when(mockUnusedApplicationsRepository.applicationsByEnvironment(Environment.SANDBOX)).thenReturn(Future(List.empty))

      val insertCaptor: ArgumentCaptor[Seq[UnusedApplication]] = ArgumentCaptor.forClass(classOf[Seq[UnusedApplication]])
      when(mockUnusedApplicationsRepository.bulkInsert(insertCaptor.capture())(*)).thenReturn(Future.successful(MultiBulkWriteResult.empty))

      await(underTest.runJob)

      val capturedInsertValue = insertCaptor.getValue
      capturedInsertValue.size must be (2)
      capturedInsertValue must contain (applicationWithLastUseDate._2)
      capturedInsertValue must contain (applicationWithoutLastUseDate._2)

      verifyNoInteractions(mockProductionThirdPartyApplicationConnector)
    }

    "not persist application details already stored in database" in new SandboxJobSetup {
      val application: (ApplicationUsageDetails, UnusedApplication) =
        applicationDetails(Environment.SANDBOX, DateTime.now.minusMonths(13), Some(DateTime.now.minusMonths(13)))

      when(mockSandboxThirdPartyApplicationConnector.applicationsLastUsedBefore(*))
        .thenReturn(Future.successful(List(application._1)))
      when(mockUnusedApplicationsRepository.applicationsByEnvironment(Environment.SANDBOX)).thenReturn(Future(List(application._2)))

      await(underTest.runJob)

      verify(mockUnusedApplicationsRepository, times(0)).bulkInsert(*)(*)
      verifyNoInteractions(mockProductionThirdPartyApplicationConnector)
    }

    "remove applications that have been used since last update" in new SandboxJobSetup {
      val application: (ApplicationUsageDetails, UnusedApplication) =
        applicationDetails(Environment.SANDBOX, DateTime.now.minusMonths(13), Some(DateTime.now.minusMonths(13)))

      when(mockSandboxThirdPartyApplicationConnector.applicationsLastUsedBefore(*)).thenReturn(Future.successful(List.empty))
      when(mockUnusedApplicationsRepository.applicationsByEnvironment(Environment.SANDBOX)).thenReturn(Future(List(application._2)))
      when(mockUnusedApplicationsRepository.deleteApplication(Environment.SANDBOX, application._2.applicationId)).thenReturn(Future.successful(true))

      await(underTest.runJob)

      verify(mockUnusedApplicationsRepository).deleteApplication(Environment.SANDBOX, application._2.applicationId)
      verifyNoInteractions(mockProductionThirdPartyApplicationConnector)
    }
  }

  "PRODUCTION job" should {
    "add all newly discovered unused applications to database" in new ProductionJobSetup {
      val applicationWithLastUseDate: (ApplicationUsageDetails, UnusedApplication) =
        applicationDetails(Environment.PRODUCTION, DateTime.now.minusMonths(13), Some(DateTime.now.minusMonths(13)))
      val applicationWithoutLastUseDate: (ApplicationUsageDetails, UnusedApplication) =
        applicationDetails(Environment.PRODUCTION, DateTime.now.minusMonths(13), None)

      when(mockProductionThirdPartyApplicationConnector.applicationsLastUsedBefore(*))
        .thenReturn(Future.successful(List(applicationWithLastUseDate._1, applicationWithoutLastUseDate._1)))
      when(mockUnusedApplicationsRepository.applicationsByEnvironment(Environment.PRODUCTION)).thenReturn(Future(List.empty))

      val insertCaptor: ArgumentCaptor[Seq[UnusedApplication]] = ArgumentCaptor.forClass(classOf[Seq[UnusedApplication]])
      when(mockUnusedApplicationsRepository.bulkInsert(insertCaptor.capture())(*)).thenReturn(Future.successful(MultiBulkWriteResult.empty))

      await(underTest.runJob)

      val capturedInsertValue = insertCaptor.getValue
      capturedInsertValue.size must be (2)
      capturedInsertValue must contain (applicationWithLastUseDate._2)
      capturedInsertValue must contain (applicationWithoutLastUseDate._2)

      verifyNoInteractions(mockSandboxThirdPartyApplicationConnector)
    }

    "not persist application details already stored in database" in new ProductionJobSetup {
      val application: (ApplicationUsageDetails, UnusedApplication) =
        applicationDetails(Environment.PRODUCTION, DateTime.now.minusMonths(13), Some(DateTime.now.minusMonths(13)))

      when(mockProductionThirdPartyApplicationConnector.applicationsLastUsedBefore(*))
        .thenReturn(Future.successful(List(application._1)))
      when(mockUnusedApplicationsRepository.applicationsByEnvironment(Environment.PRODUCTION)).thenReturn(Future(List(application._2)))

      await(underTest.runJob)

      verify(mockUnusedApplicationsRepository, times(0)).bulkInsert(*)(*)
      verifyNoInteractions(mockSandboxThirdPartyApplicationConnector)
    }

    "remove applications that have been used since last update" in new ProductionJobSetup {
      val application: (ApplicationUsageDetails, UnusedApplication) =
        applicationDetails(Environment.PRODUCTION, DateTime.now.minusMonths(13), Some(DateTime.now.minusMonths(13)))

      when(mockProductionThirdPartyApplicationConnector.applicationsLastUsedBefore(*)).thenReturn(Future.successful(List.empty))
      when(mockUnusedApplicationsRepository.applicationsByEnvironment(Environment.PRODUCTION)).thenReturn(Future(List(application._2)))
      when(mockUnusedApplicationsRepository.deleteApplication(Environment.PRODUCTION, application._2.applicationId)).thenReturn(Future.successful(true))

      await(underTest.runJob)

      verify(mockUnusedApplicationsRepository).deleteApplication(Environment.PRODUCTION, application._2.applicationId)
      verifyNoInteractions(mockSandboxThirdPartyApplicationConnector)
    }
  }

  def applicationDetails(environment: Environment, creationDate: DateTime, lastAccessDate: Option[DateTime]): (ApplicationUsageDetails, UnusedApplication) = {
    val applicationId = UUID.randomUUID()

    (ApplicationUsageDetails(applicationId, creationDate, lastAccessDate),
      UnusedApplication(applicationId, environment, lastAccessDate.getOrElse(creationDate)))
  }
}
