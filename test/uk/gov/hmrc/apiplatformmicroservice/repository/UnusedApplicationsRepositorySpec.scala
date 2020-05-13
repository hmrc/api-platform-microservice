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

package uk.gov.hmrc.apiplatformmicroservice.repository

import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import org.joda.time.DateTime
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.Index
import reactivemongo.api.indexes.IndexType.Ascending
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.{Environment, UnusedApplication}
import uk.gov.hmrc.apiplatformmicroservice.util.AsyncHmrcSpec
import uk.gov.hmrc.mongo.{MongoConnector, MongoSpecSupport}

import scala.concurrent.ExecutionContext.Implicits.global

class UnusedApplicationsRepositorySpec extends AsyncHmrcSpec
  with MongoSpecSupport
  with BeforeAndAfterEach with BeforeAndAfterAll
  with IndexVerification {

  implicit var s : ActorSystem = ActorSystem("test")
  implicit var m : Materializer = ActorMaterializer()

  private val reactiveMongoComponent = new ReactiveMongoComponent {
    override def mongoConnector: MongoConnector = mongoConnectorForTest
  }

  private val unusedApplicationRepository = new UnusedApplicationsRepository(reactiveMongoComponent)

  override def beforeEach() {
    await(unusedApplicationRepository.drop)
    await(unusedApplicationRepository.ensureIndexes)
  }

  override protected def afterAll() {
    await(unusedApplicationRepository.drop)
  }

  trait Setup {
    def sandboxApplication(applicationId: UUID) = UnusedApplication(applicationId, Environment.SANDBOX, DateTime.now)
    def productionApplication(applicationId: UUID) = UnusedApplication(applicationId, Environment.PRODUCTION, DateTime.now)
  }

  "The 'unusedApplications' collection" should {
    "have all the current indexes" in {

      val expectedIndexes = Set(
        Index(key = Seq("environment" -> Ascending, "applicationId" -> Ascending), name = Some("applicationIdIndex"), unique = true, background = true),
        Index(key = Seq("lastInteractionDate" -> Ascending), name = Some("lastInteractionDateIndex"), unique = false, background = true)
      )

      verifyIndexesVersionAgnostic(unusedApplicationRepository, expectedIndexes)
    }
  }

  "applicationsByEnvironment" should {


    "correctly retrieve SANDBOX applications" in new Setup {
      val sandboxApplicationId = UUID.randomUUID

      await(unusedApplicationRepository
        .bulkInsert(
          Seq(
            sandboxApplication(sandboxApplicationId),
            productionApplication(UUID.randomUUID()),
            productionApplication(UUID.randomUUID()))))

      val results = await(unusedApplicationRepository.applicationsByEnvironment(Environment.SANDBOX))

      results.size should be (1)
      results.head.applicationId should be (sandboxApplicationId)
    }

    "correctly retrieve PRODUCTION applications" in new Setup {
      val productionApplication1Id = UUID.randomUUID
      val productionApplication2Id = UUID.randomUUID

      await(unusedApplicationRepository
        .bulkInsert(
          Seq(
            sandboxApplication(UUID.randomUUID()),
            productionApplication(productionApplication1Id),
            productionApplication(productionApplication2Id))))

      val results = await(unusedApplicationRepository.applicationsByEnvironment(Environment.PRODUCTION))

      results.size should be (2)
      val returnedApplicationIds = results.map(_.applicationId)
      returnedApplicationIds should contain (productionApplication1Id)
      returnedApplicationIds should contain (productionApplication2Id)
    }
  }

  "deleteApplication" should {
    "correctly remove a SANDBOX application" in new Setup {
      val sandboxApplicationId = UUID.randomUUID

      await(unusedApplicationRepository
        .bulkInsert(
          Seq(
            sandboxApplication(sandboxApplicationId),
            productionApplication(UUID.randomUUID()),
            productionApplication(UUID.randomUUID()))))

      val result = await(unusedApplicationRepository.deleteApplication(Environment.SANDBOX, sandboxApplicationId))

      result should be (true)
      await(unusedApplicationRepository.applicationsByEnvironment(Environment.SANDBOX)).size should be (0)
    }

    "correctly remove a PRODUCTION application" in new Setup {
      val productionApplicationId = UUID.randomUUID

      await(unusedApplicationRepository
        .bulkInsert(
          Seq(
            sandboxApplication(UUID.randomUUID()),
            productionApplication(productionApplicationId))))

      val result = await(unusedApplicationRepository.deleteApplication(Environment.PRODUCTION, productionApplicationId))

      result should be (true)
      await(unusedApplicationRepository.applicationsByEnvironment(Environment.PRODUCTION)).size should be (0)
    }

    "return true if application id was not found in database" in {
      val result = await(unusedApplicationRepository.deleteApplication(Environment.PRODUCTION, UUID.randomUUID()))

      result should be (true)
    }
  }
}
