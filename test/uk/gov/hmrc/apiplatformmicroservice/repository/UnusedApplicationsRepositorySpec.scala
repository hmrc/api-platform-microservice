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

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.Index
import reactivemongo.api.indexes.IndexType.Ascending
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

  "The 'unusedApplications' collection" should {
    "have all the current indexes" in {

      val expectedIndexes = Set(
        Index(key = Seq("applicationId" -> Ascending), name = Some("applicationIdIndex"), unique = true, background = true),
        Index(key = Seq("lastInteractionDate" -> Ascending), name = Some("lastInteractionDateIndex"), unique = false, background = true)
      )

      verifyIndexesVersionAgnostic(unusedApplicationRepository, expectedIndexes)
    }
  }
}
