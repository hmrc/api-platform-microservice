/*
 * Copyright 2021 HM Revenue & Customs
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

import uk.gov.hmrc.apiplatformmicroservice.common.ApplicationLogger
import uk.gov.hmrc.apiplatformmicroservice.xmlapis.models.{BasicXmlApisJsonFormatters, XmlApi}

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.http.HttpReads.Implicits._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal


@Singleton
class XmlApisConnector @Inject()(httpClient: HttpClient, appConfig: XmlApisConnector.Config)
                                (implicit ec: ExecutionContext) extends BasicXmlApisJsonFormatters with ApplicationLogger {

  private lazy val serviceBaseUrl: String = appConfig.serviceBaseUrl

  def fetchAllXmlApis()(implicit hc: HeaderCarrier): Future[Seq[XmlApi]] = {

    logger.info(s"${this.getClass.getSimpleName} - fetchAllXmlApis")

    val r = httpClient.GET[Seq[XmlApi]](s"$serviceBaseUrl/api-platform-xml-services/xml/apis")

    r.recover {
      case NonFatal(e) =>
        logger.error(s"Failed $e")
        throw e
    }
  }



  def fetchXmlApiByName(name: String)(implicit hc: HeaderCarrier): Future[Option[XmlApi]] = {
    logger.info(s"${this.getClass.getSimpleName} - fetchXmlApiByName $name")
    val r = httpClient.GET[Option[XmlApi]](s"$serviceBaseUrl/api-platform-xml-services/xml/api/$name")

    r.recover {
      case NonFatal(e) =>
        logger.error(s"Failed $e")
        throw e
    }
  }

}

object XmlApisConnector {
  case class Config(serviceBaseUrl: String)
}


