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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services

import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.services.ApplicationJsonFormatters
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.ApplicationId
import uk.gov.hmrc.apiplatformmicroservice.common.builder.ApplicationBuilder
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress
import uk.gov.hmrc.apiplatformmicroservice.common.utils.CollaboratorTracker
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks.CommandConnectorMockModule
import org.mockito.MockitoSugar
import org.mockito.ArgumentMatchersSugar
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.EnvironmentAwareAppCmdConnector
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.DispatchRequest
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.Environment
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.ApplicationCommands
import uk.gov.hmrc.apiplatform.modules.common.domain.models.Actors
import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.CommandFailures
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.DispatchSuccessResult
import cats.data.NonEmptyChain

class PassThruDispatcherSpec extends AsyncHmrcSpec {

  trait Setup 
      extends MockitoSugar
      with ArgumentMatchersSugar
      with ApplicationBuilder
      with CollaboratorTracker
      with ApplicationJsonFormatters
      with CommandConnectorMockModule
      with FixedClock {

    implicit val headerCarrier = HeaderCarrier()
    
    val sandboxApplicationId = ApplicationId.random
    val sandboxApplication = buildApplication(appId = sandboxApplicationId)
    val productionApplicationId = ApplicationId.random
    val productionApplication = buildApplication(appId = productionApplicationId).copy(deployedTo = Environment.PRODUCTION)

    val adminEmail = "admin@example.com".toLaxEmail
    val developerAsCollaborator = "dev@example.com".toLaxEmail.asDeveloperCollaborator
    val verifiedEmails = Set.empty[LaxEmailAddress]
    val envAwareCmdConnector = new EnvironmentAwareAppCmdConnector(CommandConnectorMocks.Sandbox.aMock, CommandConnectorMocks.Prod.aMock)

    val passThruDispatcher: PassThruDispatcher = new PassThruDispatcher(envAwareCmdConnector)
  }

  "PassThruDispatcher" should {
    "dispatch a command when the app exists in sandbox" in new Setup{

      CommandConnectorMocks.Sandbox.IssueCommand.Dispatch.succeedsWith(sandboxApplication)

      val cmd = ApplicationCommands.AddCollaborator(Actors.AppCollaborator(adminEmail), developerAsCollaborator, now)

      await(passThruDispatcher.dispatch(sandboxApplication, DispatchRequest(cmd, Set.empty))).right.value shouldBe DispatchSuccessResult(sandboxApplication)

      CommandConnectorMocks.Prod.IssueCommand.verifyNoCommandsIssued()
      CommandConnectorMocks.Sandbox.IssueCommand.verifyCalledWith(cmd, verifiedEmails)
    }
    
    "dispatch a command when the app exists in production" in new Setup{

      CommandConnectorMocks.Prod.IssueCommand.Dispatch.succeedsWith(productionApplication)

      val cmd = ApplicationCommands.AddCollaborator(Actors.AppCollaborator(adminEmail), developerAsCollaborator, now)
      
      await(passThruDispatcher.dispatch(productionApplication, DispatchRequest(cmd, Set.empty))).right.value shouldBe DispatchSuccessResult(productionApplication)

      CommandConnectorMocks.Sandbox.IssueCommand.verifyNoCommandsIssued()
      CommandConnectorMocks.Prod.IssueCommand.verifyCalledWith(cmd, verifiedEmails)
    }

    "dispatch a command and handle command failure" in new Setup{

      CommandConnectorMocks.Sandbox.IssueCommand.Dispatch.failsWith(CommandFailures.ActorIsNotACollaboratorOnApp)

      val cmd = ApplicationCommands.AddCollaborator(Actors.AppCollaborator(adminEmail), developerAsCollaborator, now)

      await(passThruDispatcher.dispatch(sandboxApplication, DispatchRequest(cmd, Set.empty))).left.value shouldBe NonEmptyChain.one(CommandFailures.ActorIsNotACollaboratorOnApp)

      CommandConnectorMocks.Sandbox.IssueCommand.verifyCalledWith(cmd, verifiedEmails)
      CommandConnectorMocks.Prod.IssueCommand.verifyNoCommandsIssued()
    }

  }
}
