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

package uk.gov.hmrc.apiplatformmicroservice.apidefinition.connectors

import org.joda.time.DateTime
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.OK
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.connectors.ThirdPartyDeveloperConnector.JsonFormatters.{formatDeleteDeveloperRequest, formatDeleteUnregisteredDevelopersRequest}
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.connectors.ThirdPartyDeveloperConnector.{DeleteDeveloperRequest, DeleteUnregisteredDevelopersRequest, DeveloperResponse, ThirdPartyDeveloperConnectorConfig}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.time.DateTimeUtils.now

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Future.successful

class ThirdPartyDeveloperConnectorSpec extends UnitSpec with ScalaFutures with MockitoSugar {

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val mockHttp: HttpClient = mock[HttpClient]
    val baseUrl = "http://third-party-developer"
    val config = ThirdPartyDeveloperConnectorConfig(baseUrl)
    val devEmail = "joe.bloggs@example.com"
    def endpoint(path: String) = s"$baseUrl/$path"

    val connector = new ThirdPartyDeveloperConnector(config, mockHttp)
  }

  "fetchUnverifiedDevelopers" should {
    val limit = 10
    "return developer emails" in new Setup {
      when(mockHttp.GET[Seq[DeveloperResponse]](meq(endpoint("developers")),
        meq(Seq("createdBefore" -> "20200201", "limit" -> s"$limit", "status" -> "UNVERIFIED")))(any(), any(), any()))
        .thenReturn(successful(Seq(DeveloperResponse(devEmail))))

      val result: Seq[String] = await(connector.fetchUnverifiedDevelopers(new DateTime(2020, 2, 1, 0, 0), limit))

      result shouldBe Seq(devEmail)
    }

    "propagate error when endpoint returns error" in new Setup {
      when(mockHttp.GET[Seq[DeveloperResponse]](meq(endpoint("developers")), any())(any(), any(), any())).thenReturn(Future.failed(new NotFoundException("")))

      intercept[NotFoundException] {
        await(connector.fetchUnverifiedDevelopers(now, limit))
      }
    }
  }

  "fetchExpiredUnregisteredDevelopers" should {
    val limit = 10
    "return developer emails" in new Setup {
      when(mockHttp.GET[Seq[DeveloperResponse]](meq(endpoint("unregistered-developer/expired")), meq(Seq("limit" -> s"$limit")))(any(), any(), any()))
        .thenReturn(successful(Seq(DeveloperResponse(devEmail))))

      val result: Seq[String] = await(connector.fetchExpiredUnregisteredDevelopers(limit))

      result shouldBe Seq(devEmail)
    }

    "propagate error when endpoint returns error" in new Setup {
      when(mockHttp.GET[Seq[DeveloperResponse]](meq(endpoint("unregistered-developer/expired")), any())(any(), any(), any())).thenReturn(Future.failed(new NotFoundException("")))

      intercept[NotFoundException] {
        await(connector.fetchExpiredUnregisteredDevelopers(limit))
      }
    }
  }

  "deleteDeveloper" should {
    "delete developer" in new Setup {
      when(mockHttp.POST(endpoint("developer/delete?notifyDeveloper=false"), DeleteDeveloperRequest(devEmail))).thenReturn(successful(HttpResponse(OK)))

      val result: Int = await(connector.deleteDeveloper(devEmail))

      result shouldBe OK
    }

    "propagate error when endpoint returns error" in new Setup {
      when(mockHttp.POST(endpoint("developer/delete?notifyDeveloper=false"), DeleteDeveloperRequest(devEmail))).thenReturn(Future.failed(new NotFoundException("")))

      intercept[NotFoundException] {
        await(connector.deleteDeveloper(devEmail))
      }
    }
  }

  "deleteUnregisteredDeveloper" should {
    "delete unregistered developer" in new Setup {
      when(mockHttp.POST(endpoint("unregistered-developer/delete"), DeleteUnregisteredDevelopersRequest(Seq(devEmail)))).thenReturn(successful(HttpResponse(OK)))

      val result: Int = await(connector.deleteUnregisteredDeveloper(devEmail))

      result shouldBe OK
    }

    "propagate error when endpoint returns error" in new Setup {
      when(mockHttp.POST(endpoint("unregistered-developer/delete"), DeleteUnregisteredDevelopersRequest(Seq(devEmail)))).thenReturn(Future.failed(new NotFoundException("")))

      intercept[NotFoundException] {
        await(connector.deleteUnregisteredDeveloper(devEmail))
      }
    }
  }
}
