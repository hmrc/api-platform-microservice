/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.apiplatformmicroservice.xmlapis.connectors

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}

import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ServiceName
import uk.gov.hmrc.apiplatformmicroservice.common.ApplicationLogger
import uk.gov.hmrc.apiplatformmicroservice.common.connectors.ConnectorRecovery
import uk.gov.hmrc.apiplatformmicroservice.xmlapis.models.{BasicXmlApisJsonFormatters, XmlApi}

@Singleton
class XmlApisConnector @Inject() (http: HttpClientV2, appConfig: XmlApisConnector.Config)(implicit ec: ExecutionContext) extends BasicXmlApisJsonFormatters
    with ApplicationLogger
    with ConnectorRecovery {

  private lazy val serviceBaseUrl: String = appConfig.serviceBaseUrl

  def fetchAllXmlApis()(implicit hc: HeaderCarrier): Future[Seq[XmlApi]] = {
    logger.info(s"${this.getClass.getSimpleName} - fetchAllXmlApis")
    http.get(url"$serviceBaseUrl/api-platform-xml-services/xml/apis")
      .execute[Seq[XmlApi]]
      .recover(recovery)
  }

  def fetchXmlApiByServiceName(serviceName: ServiceName)(implicit hc: HeaderCarrier): Future[Option[XmlApi]] = {
    logger.info(s"${this.getClass.getSimpleName} - fetchXmlApiByName $serviceName")
    http.get(url"$serviceBaseUrl/api-platform-xml-services/xml/api?serviceName=$serviceName")
      .execute[Option[XmlApi]]
      .recover(recovery)
  }

}

object XmlApisConnector {
  case class Config(serviceBaseUrl: String)
}
