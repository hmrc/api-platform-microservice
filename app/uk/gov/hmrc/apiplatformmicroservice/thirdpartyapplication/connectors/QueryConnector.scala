/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors

import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2

import uk.gov.hmrc.apiplatform.modules.common.domain.models.Environment
import uk.gov.hmrc.apiplatform.modules.applications.query.domain.models.ApplicationQuery
import uk.gov.hmrc.apiplatformmicroservice.metrics._

object QueryConnector {
  case class Config(tpoBaseUrl: String)
}

@Singleton
@Named("principal")
class QueryConnector @Inject() (
    val config: QueryConnector.Config,
    val http: HttpClientV2,
    val apiMetrics: ApiMetrics
  )(implicit val ec: ExecutionContext
  ) extends RecordMetrics {

  import config.tpoBaseUrl

  val api = API("third-party-orchestrator")

  def query(environment: Environment, qry: Map[String, Seq[String]])(implicit hc: HeaderCarrier): Future[HttpResponse] = {

    val simplifiedQry = qry.map {
      case (k, vs) => k -> vs.mkString
    }

    http
      .get(url"${tpoBaseUrl}/environment/$environment/query?$simplifiedQry")
      .execute[HttpResponse]
  }

  def query(qry: ApplicationQuery)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    http
      .get(url"${tpoBaseUrl}/query?$qry")
      .execute[HttpResponse]
  }
}
