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

import java.util.concurrent.TimeUnit.{HOURS, SECONDS}

import akka.stream.Materializer
import org.joda.time.Duration
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status.OK
import play.api.inject.guice.GuiceApplicationBuilder
import play.modules.reactivemongo.ReactiveMongoComponent
import uk.gov.hmrc.apiplatformmicroservice.connectors.{ProductionThirdPartyApplicationConnector, SandboxThirdPartyApplicationConnector, ThirdPartyDeveloperConnector}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lock.LockRepository
import uk.gov.hmrc.mongo.{MongoConnector, MongoSpecSupport}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future.{failed, successful}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

class MigrateUnregisteredDevelopersJobSpec extends UnitSpec with MockitoSugar with MongoSpecSupport with GuiceOneAppPerSuite {

  implicit override lazy val app: Application = new GuiceApplicationBuilder()
    .disable[com.kenshoo.play.metrics.PlayModule]
    .configure("metrics.enabled" -> false).build()
  implicit lazy val materializer: Materializer = app.materializer

  trait Setup {
    implicit val hc = HeaderCarrier()
    val lockKeeperSuccess: () => Boolean = () => true
    private val reactiveMongoComponent = new ReactiveMongoComponent {
      override def mongoConnector: MongoConnector = mongoConnectorForTest
    }

    val mockLockKeeper: MigrateUnregisteredDevelopersJobLockKeeper = new MigrateUnregisteredDevelopersJobLockKeeper(reactiveMongoComponent) {
      override def lockId: String = "testLock"
      override def repo: LockRepository = mock[LockRepository]
      override val forceLockReleaseAfter: Duration = Duration.standardMinutes(5) // scalastyle:off magic.number
      override def tryLock[T](body: => Future[T])(implicit ec: ExecutionContext): Future[Option[T]] =
        if (lockKeeperSuccess()) body.map(value => successful(Some(value)))
        else successful(None)
    }

    val migrateUnregisteredDevelopersJobConfig: MigrateUnregisteredDevelopersJobConfig = MigrateUnregisteredDevelopersJobConfig(
      FiniteDuration(60, SECONDS), FiniteDuration(24, HOURS), enabled = true)
    val mockThirdPartyDeveloperConnector: ThirdPartyDeveloperConnector = mock[ThirdPartyDeveloperConnector]
    val mockSandboxThirdPartyApplicationConnector: SandboxThirdPartyApplicationConnector = mock[SandboxThirdPartyApplicationConnector]
    val mockProductionThirdPartyApplicationConnector: ProductionThirdPartyApplicationConnector = mock[ProductionThirdPartyApplicationConnector]
    val underTest = new MigrateUnregisteredDevelopersJob(
      mockLockKeeper,
      migrateUnregisteredDevelopersJobConfig,
      mockThirdPartyDeveloperConnector,
      mockSandboxThirdPartyApplicationConnector,
      mockProductionThirdPartyApplicationConnector
    )
  }

  "MigrateUnregisteredDevelopersJob" should {
    import scala.concurrent.ExecutionContext.Implicits.global

    "migrate unregistered developers" in new Setup {
      when(mockSandboxThirdPartyApplicationConnector.fetchAllCollaborators(any()))
        .thenReturn(successful(Set("joe.bloggs@example.com", "sandbox.user@example.com")))
      when(mockProductionThirdPartyApplicationConnector.fetchAllCollaborators(any()))
        .thenReturn(successful(Set("joe.bloggs@example.com", "production.user@example.com")))
      when(mockThirdPartyDeveloperConnector.createUnregisteredDeveloper(any())(any())).thenReturn(successful(OK))

      val result: underTest.Result = await(underTest.execute)

      result.message shouldBe "MigrateUnregisteredDevelopersJob Job ran successfully."
      verify(mockThirdPartyDeveloperConnector, times(1)).createUnregisteredDeveloper(meq("joe.bloggs@example.com"))(any())
      verify(mockThirdPartyDeveloperConnector, times(1)).createUnregisteredDeveloper(meq("sandbox.user@example.com"))(any())
      verify(mockThirdPartyDeveloperConnector, times(1)).createUnregisteredDeveloper(meq("production.user@example.com"))(any())
      verifyNoMoreInteractions(mockThirdPartyDeveloperConnector)
    }

    "not execute if the job is already running" in new Setup {
      override val lockKeeperSuccess: () => Boolean = () => false

      val result: underTest.Result = await(underTest.execute)

      verify(mockSandboxThirdPartyApplicationConnector, never).fetchAllCollaborators(any())
      verify(mockProductionThirdPartyApplicationConnector, never).fetchAllCollaborators(any())
      verify(mockThirdPartyDeveloperConnector, never).createUnregisteredDeveloper(any())(any())
      result.message shouldBe "MigrateUnregisteredDevelopersJob did not run because repository was locked by another instance of the scheduler."
    }

    "handle error when something fails" in new Setup {
      when(mockSandboxThirdPartyApplicationConnector.fetchAllCollaborators(any())).thenReturn(failed(new RuntimeException("Failed")))

      val result: underTest.Result = await(underTest.execute)

      verify(mockThirdPartyDeveloperConnector, never).createUnregisteredDeveloper(any())(any())
      result.message shouldBe "The execution of scheduled job MigrateUnregisteredDevelopersJob failed with error 'Failed'. " +
        "The next execution of the job will do retry."
    }
  }
}
