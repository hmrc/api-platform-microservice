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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

import play.api.Logger
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.ApiIdentifier
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications._
import scala.util.{Failure, Success}

@Singleton
class SubscriptionFieldsService @Inject() (supplier: EnvironmentAwareConnectorsSupplier)(implicit val ec: ExecutionContext) {

  def logAndReturn[T](tag: String)(f: => Future[T]): Future[T] = {
    val v = f
    v.onComplete {
      case Success(s) => Logger.info(s"****$tag : Success " + s)
      case Failure(f) => Logger.info(s"****$tag : Failed " + f)
    }
    v
  }

  def fetchFieldsValues(application: Application, apiIdentifier: ApiIdentifier)(implicit hc: HeaderCarrier): Future[Map[FieldName, FieldValue]] = {
    fetchFieldsValues(application.clientId, application.deployedTo, apiIdentifier)
  }

  def fetchFieldsValues(clientId: ClientId, environment: Environment, apiIdentifier: ApiIdentifier)(implicit hc: HeaderCarrier): Future[Map[FieldName, FieldValue]] = {
    val connector = supplier.forEnvironment(environment).apiSubscriptionFieldsConnector
    connector.fetchFieldValues(clientId, apiIdentifier)
  }

}

object SubscriptionFieldsService {

  trait SubscriptionFieldsConnector {
    def fetchFieldValues(clientId: ClientId, apiIdentifier: ApiIdentifier)(implicit hc: HeaderCarrier): Future[Map[FieldName, FieldValue]]
  }
}
