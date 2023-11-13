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

package uk.gov.hmrc.apiplatformmicroservice.commands.applications.services

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future.successful

import cats.data.NonEmptyList
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{Actors, _}
import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock
import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.Access
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.{ApplicationCommands, CommandFailures, DispatchRequest}
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiDefinitionTestDataHelper
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.services.ApiDefinitionsForApplicationFetcher
import uk.gov.hmrc.apiplatformmicroservice.common.builder.ApplicationBuilder
import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks._

class SubscribeToApiPreprocessorSpec extends AsyncHmrcSpec with ApiDefinitionTestDataHelper with ApplicationBuilder with FixedClock {

  val apiDefinitionOne     = apiDefinition("one")
  val apiDefinitionTwo     = apiDefinition("two")
  val apiDefinitionThree   = apiDefinition("three")
  val apiDefinitionPrivate = apiDefinition("four").asPrivate

  val apiDefintions = Seq(apiDefinitionOne, apiDefinitionTwo, apiDefinitionThree, apiDefinitionPrivate)

  val apiVersionOne        = ApiVersionNbr("1.0")
  val apiVersionTwo        = ApiVersionNbr("2.0")
  val apiIdentifierOne     = ApiIdentifier(apiDefinitionOne.context, apiVersionOne)
  val apiIdentifierTwo     = ApiIdentifier(apiDefinitionTwo.context, apiVersionOne)
  val apiIdentifierThree   = ApiIdentifier(apiDefinitionThree.context, apiVersionOne)
  val apiIdentifierPrivate = ApiIdentifier(apiDefinitionPrivate.context, apiVersionOne)

  val applicationId = ApplicationId.random
  val anApplication = buildApplication(appId = applicationId)

  val goodApi = apiIdentifierThree

  implicit val hc: HeaderCarrier = HeaderCarrier()

  trait Setup
      extends SubscriptionFieldsConnectorModule
      with ApplicationByIdFetcherModule
      with SubscriptionFieldsServiceModule
      with MockitoSugar
      with ArgumentMatchersSugar {

    val mockApiDefinitionsForApplicationFetcher = mock[ApiDefinitionsForApplicationFetcher]

    val preprocessor = new SubscribeToApiPreprocessor(
      mockApiDefinitionsForApplicationFetcher,
      EnvironmentAwareSubscriptionFieldsConnectorMock.instance,
      ApplicationByIdFetcherMock.aMock,
      SubscriptionFieldsServiceMock.aMock
    )
  }
  "SubscribeToApiPreprocessor" should {
    val data = Set("x".toLaxEmail)
    val cmd  = ApplicationCommands.SubscribeToApi(Actors.Unknown, goodApi, now)

    "fail if ropc app and not a GK actor" in new Setup {
      val application = anApplication.copy(access = Access.Ropc())

      await(preprocessor.process(application, cmd, data).value).left.value shouldBe NonEmptyList.one(CommandFailures.SubscriptionNotAvailable)
    }

    "fail if priviledged app and not a GK actor" in new Setup {
      val application = anApplication.copy(access = Access.Privileged())

      await(preprocessor.process(application, cmd, data).value).left.value shouldBe NonEmptyList.one(CommandFailures.SubscriptionNotAvailable)
    }

    "fail if aleady subscribed" in new Setup {
      val application      = anApplication
      val cmdWithDuplicate = cmd.copy(apiIdentifier = apiIdentifierOne)

      ApplicationByIdFetcherMock.FetchApplicationWithSubscriptionData.willReturnApplicationWithSubscriptionData(application, Set(apiIdentifierOne, apiIdentifierTwo))

      await(preprocessor.process(application, cmdWithDuplicate, data).value).left.value shouldBe NonEmptyList.one(CommandFailures.DuplicateSubscription)
    }

    "fail if denied due to private and restricted" in new Setup {
      val application    = anApplication
      val cmdWithPrivate = cmd.copy(apiIdentifier = apiIdentifierPrivate)

      ApplicationByIdFetcherMock.FetchApplicationWithSubscriptionData.willReturnApplicationWithSubscriptionData(application, Set(apiIdentifierOne, apiIdentifierTwo))
      when(mockApiDefinitionsForApplicationFetcher.fetch(*, *, *)(*)).thenReturn(successful(apiDefintions.toList))

      await(preprocessor.process(application, cmdWithPrivate, data).value).left.value shouldBe NonEmptyList.one(CommandFailures.SubscriptionNotAvailable)
    }

    "fail if create field values fails" in new Setup {
      val application = anApplication

      ApplicationByIdFetcherMock.FetchApplicationWithSubscriptionData.willReturnApplicationWithSubscriptionData(application, Set(apiIdentifierOne, apiIdentifierTwo))
      when(mockApiDefinitionsForApplicationFetcher.fetch(*, *, *)(*)).thenReturn(successful(apiDefintions.toList))
      SubscriptionFieldsServiceMock.CreateFieldValues.fails()

      await(preprocessor.process(application, cmd, data).value).left.value shouldBe NonEmptyList.one(CommandFailures.GenericFailure("Creation of field values failed"))
    }

    "pass with private api when from gatekeeper" in new Setup {
      val application    = anApplication
      val cmdWithPrivate = cmd.copy(actor = Actors.GatekeeperUser("Bob"), apiIdentifier = apiIdentifierPrivate)

      ApplicationByIdFetcherMock.FetchApplicationWithSubscriptionData.willReturnApplicationWithSubscriptionData(application, Set(apiIdentifierOne, apiIdentifierTwo))
      when(mockApiDefinitionsForApplicationFetcher.fetch(*, *, *)(*)).thenReturn(successful(apiDefintions.toList))
      SubscriptionFieldsServiceMock.CreateFieldValues.succeeds()

      await(preprocessor.process(application, cmdWithPrivate, data).value).value shouldBe DispatchRequest(cmdWithPrivate, data)
    }

    "pass on the request if everything is okay" in new Setup {
      val application = anApplication

      ApplicationByIdFetcherMock.FetchApplicationWithSubscriptionData.willReturnApplicationWithSubscriptionData(application, Set(apiIdentifierOne, apiIdentifierTwo))
      when(mockApiDefinitionsForApplicationFetcher.fetch(*, *, *)(*)).thenReturn(successful(apiDefintions.toList))
      SubscriptionFieldsServiceMock.CreateFieldValues.succeeds()

      await(preprocessor.process(application, cmd, data).value).value shouldBe DispatchRequest(cmd, data)
    }
  }
}
