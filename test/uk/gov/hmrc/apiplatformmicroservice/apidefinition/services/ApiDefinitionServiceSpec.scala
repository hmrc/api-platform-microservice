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

package uk.gov.hmrc.apiplatformmicroservice.apidefinition.services

import scala.concurrent.Future

import org.scalatest.prop.TableDrivenPropertyChecks._

import play.api.libs.ws.WSResponse
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatformmicroservice.apidefinition.connectors.{ApiDefinitionConnector, PrincipalApiDefinitionConnector, SubordinateApiDefinitionConnector}
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.{ApiCategoryDetails, ApiDefinition, ApiDefinitionTestDataHelper, ApiVersion, ResourceId}
import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.apiplatformmicroservice.metrics.{API, ApiMetrics, NoopTimer}

class ApiDefinitionServiceSpec extends AsyncHmrcSpec with ApiDefinitionTestDataHelper {

  import scala.concurrent.ExecutionContext.Implicits.global

  private val serviceName = "test-service"
  private val versionOne  = ApiVersion("1.0")
  private val resource    = "/mock/resourcename"

  private val api1 = apiDefinition("Bob")
  private val api2 = apiDefinition("Charlie").withClosedAccess
  private val api3 = apiDefinition("Dannie").asPrivate

  private val resourceId = ResourceId(serviceName, versionOne, resource)

  implicit val hc = HeaderCarrier()

  trait MockApiMetrics {
    val mockApiMetrics = mock[ApiMetrics]
    doNothing.when(mockApiMetrics).recordFailure(any[API])
    doNothing.when(mockApiMetrics).recordSuccess(any[API])
    when(mockApiMetrics.startTimer(any[API])).thenReturn(NoopTimer)
  }

  trait AbstractSetup extends MockApiMetrics {
    def mockConnector: ApiDefinitionConnector

    def svc: ApiDefinitionService
  }

  class Setup(isEnabled: Boolean = true) extends AbstractSetup {

    val mockConnector = mock[ApiDefinitionConnector]

    val svc: ApiDefinitionService = new ApiDefinitionService {
      val connector = mockConnector

      val apiMetrics = mockApiMetrics

      val api = API("Mock-API")

      val enabled = isEnabled
    }
  }

  class SetupPrincipal extends AbstractSetup {

    val mockConnector = mock[PrincipalApiDefinitionConnector]

    val svc: ApiDefinitionService =
      new PrincipalApiDefinitionService(mockConnector, mockApiMetrics)
  }

  class SetupSubordinate(isEnabled: Boolean) extends AbstractSetup {

    val mockConnector = mock[SubordinateApiDefinitionConnector]

    val config = SubordinateApiDefinitionService.Config(enabled = isEnabled)

    val svc: ApiDefinitionService =
      new SubordinateApiDefinitionService(mockConnector, config, mockApiMetrics)
  }

  val disabledScenarios =
    Table(
      ("name", "values"),
      ("ApiDefinitionService", () => new Setup(false)),
      ("SubordinateApiDefinitionService", () => new SetupSubordinate(false))
    )

  "when not enabled" should {

    forAll(disabledScenarios) { (name, setupFn) =>
      s"in $name" should {

        "return none for fetchDefinition" in {
          val obj = setupFn()
          import obj._

          val result = await(svc.fetchDefinition(serviceName))

          result shouldEqual None
        }

        "return empty list for fetchAllNonOpenAccessApiDefinitions" in {
          val obj = setupFn()
          import obj._

          val result = await(svc.fetchAllNonOpenAccessApiDefinitions)

          result shouldEqual List.empty
        }

        "return none for fetchApiDocumentationResource" in {
          val obj = setupFn()
          import obj._

          val result = await(
            svc.fetchApiDocumentationResource(
              ResourceId(serviceName, versionOne, "/any/esource")
            )
          )

          result shouldEqual None
        }
      }
    }
  }

  val enabledScenarios =
    Table(
      ("name", "values"),
      ("ApiDefinitionService", () => new Setup(true)),
      ("PrincipalApiDefinitionService", () => new SetupPrincipal),
      ("SubordinateApiDefinitionService", () => new SetupSubordinate(true))
    )

  "when enabled" should {

    forAll(enabledScenarios) { (name, setupFn) =>
      s"in $name" should {

        "return the definition in a call to fetchDefinition" in {
          val obj = setupFn()
          import obj._

          val expected   = Some(mock[ApiDefinition])
          val mockFuture = Future.successful(expected)

          when(mockConnector.fetchApiDefinition(eqTo(serviceName))(any))
            .thenReturn(mockFuture)

          val actual = await(svc.fetchDefinition(serviceName))
          actual shouldEqual expected

          verify(mockApiMetrics).recordSuccess(eqTo(svc.api))
        }

        "return an error in a call to fetchDefinition that fails" in {
          val obj = setupFn()
          import obj._

          val mockFuture = Future.failed(new RuntimeException)

          when(mockConnector.fetchApiDefinition(eqTo(serviceName))(any))
            .thenReturn(mockFuture)

          intercept[RuntimeException] {
            await(svc.fetchDefinition(serviceName))
          }

          verify(mockApiMetrics).recordFailure(eqTo(svc.api))
        }

        "return the definitions in a call to fetchAllNonOpenAccessApiDefinitions" in {
          val obj = setupFn()
          import obj._

          val mockFuture = Future.successful(List(api1, api2))

          when(mockConnector.fetchAllApiDefinitions(any))
            .thenReturn(mockFuture)

          val actual = await(svc.fetchAllNonOpenAccessApiDefinitions)
          actual shouldBe List(api2)

          verify(mockApiMetrics).recordSuccess(eqTo(svc.api))
        }

        "return an error in a call to fetchAllNonOpenAccessApiDefinitions that fails" in {
          val obj = setupFn()
          import obj._

          val mockFuture = Future.failed(new RuntimeException)

          when(mockConnector.fetchAllApiDefinitions(any))
            .thenReturn(mockFuture)

          intercept[RuntimeException] {
            await(svc.fetchAllNonOpenAccessApiDefinitions)
          }

          verify(mockApiMetrics).recordFailure(eqTo(svc.api))
        }

        "return the definitions in a call to fetchAllOpenAccessApiDefinitions" in {
          val obj = setupFn()
          import obj._

          val mockFuture = Future.successful(List(api1, api2))

          when(mockConnector.fetchAllApiDefinitions(any))
            .thenReturn(mockFuture)

          val actual = await(svc.fetchAllOpenAccessApiDefinitions)
          actual shouldBe List(api1)

          verify(mockApiMetrics).recordSuccess(eqTo(svc.api))
        }

        "return the definitions in a call to fetchAllOpenAccessApiDefinitions eliminating private access" in {
          val obj = setupFn()
          import obj._

          val mockFuture = Future.successful(List(api1, api3))

          when(mockConnector.fetchAllApiDefinitions(any))
            .thenReturn(mockFuture)

          val actual = await(svc.fetchAllOpenAccessApiDefinitions)
          actual shouldBe List(api1)

          verify(mockApiMetrics).recordSuccess(eqTo(svc.api))
        }

        "return an error in a call to fetchAllOpenAccessApiDefinitions that fails" in {
          val obj = setupFn()
          import obj._

          val mockFuture = Future.failed(new RuntimeException)

          when(mockConnector.fetchAllApiDefinitions(any))
            .thenReturn(mockFuture)

          intercept[RuntimeException] {
            await(svc.fetchAllOpenAccessApiDefinitions)
          }

          verify(mockApiMetrics).recordFailure(eqTo(svc.api))
        }

        "return the resource in a call to fetchApiDocumentationResource" in {
          val obj = setupFn()
          import obj._

          val result     = Some(mock[WSResponse])
          val mockFuture = Future.successful(result)

          when(
            mockConnector.fetchApiDocumentationResource(eqTo(resourceId))(any)
          )
            .thenReturn(mockFuture)

          await(svc.fetchApiDocumentationResource(resourceId))

          verify(mockApiMetrics).recordSuccess(eqTo(svc.api))
        }

        "return an error in a call to fetchApiDocumentationResource that fails" in {
          val obj = setupFn()
          import obj._

          val mockFuture = Future.failed(new RuntimeException)

          when(
            mockConnector.fetchApiDocumentationResource(eqTo(resourceId))(any)
          )
            .thenReturn(mockFuture)

          intercept[RuntimeException] {
            await(svc.fetchApiDocumentationResource(resourceId))
          }

          verify(mockApiMetrics).recordFailure(eqTo(svc.api))
        }

        "return the API Category details in a call to fetchAllApiCategoryDetails" in {
          val obj = setupFn()
          import obj._

          val expected   = mock[List[ApiCategoryDetails]]
          val mockFuture = Future.successful(expected)

          when(mockConnector.fetchApiCategoryDetails()(any))
            .thenReturn(mockFuture)

          val actual = await(svc.fetchAllApiCategoryDetails)
          actual shouldBe expected

          verify(mockApiMetrics).recordSuccess(eqTo(svc.api))
        }
      }
    }
  }
}
