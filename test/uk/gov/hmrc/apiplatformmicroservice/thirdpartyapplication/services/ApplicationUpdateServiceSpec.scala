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

import scala.concurrent.ExecutionContext.Implicits.global

import org.joda.time.DateTime
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatformmicroservice.common.builder.{ApplicationBuilder, UserResponseBuilder}
import uk.gov.hmrc.apiplatform.modules.developers.domain.models.UserId
import uk.gov.hmrc.apiplatformmicroservice.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.domain.UnregisteredUserResponse
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications._
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks._
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.domain.models.Actors
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.Collaborators
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.Collaborator
import uk.gov.hmrc.apiplatform.modules.common.domain.services.ClockNow
import java.time.Clock

class ApplicationUpdateServiceSpec extends AsyncHmrcSpec with ClockNow {

  val clock = Clock.systemUTC()

  import uk.gov.hmrc.apiplatform.modules.applications.domain.models.Collaborator.Roles

  implicit val hc = HeaderCarrier()

  trait Setup extends ThirdPartyApplicationConnectorModule with MockitoSugar
      with ArgumentMatchersSugar with UserResponseBuilder with ApplicationBuilder {

    val service = new ApplicationUpdateService(EnvironmentAwareThirdPartyApplicationConnectorMock.instance)

    val newCollaboratorEmail                    = "newCollaborator@testuser.com".toLaxEmail
    val newCollaboratorUserId                   = UserId.random
    val newCollaborator                         = Collaborators.Developer(newCollaboratorUserId, newCollaboratorEmail)
    val newCollaboratorUserResponse             = buildUserResponse(email = newCollaboratorEmail, userId = newCollaboratorUserId)
    val newCollaboratorUnregisteredUserResponse = UnregisteredUserResponse(newCollaboratorEmail, DateTime.now, newCollaboratorUserId)

    val requesterEmail            = "adminRequester@example.com".toLaxEmail
    val verifiedAdminEmail        = "verifiedAdmin@example.com".toLaxEmail
    val unverifiedAdminEmail      = "unverifiedAdmin@example.com".toLaxEmail
    val adminEmailsMinusRequester = Set(verifiedAdminEmail, unverifiedAdminEmail)
    val adminEmails               = Set(verifiedAdminEmail, unverifiedAdminEmail, requesterEmail)

    val adminMinusRequesterUserResponses =
      Seq(buildUserResponse(email = verifiedAdminEmail, userId = UserId.random), buildUserResponse(email = unverifiedAdminEmail, verified = false, userId = UserId.random))

    val adminUserResponses = Seq(
      buildUserResponse(email = verifiedAdminEmail, userId = UserId.random),
      buildUserResponse(email = unverifiedAdminEmail, verified = false, userId = UserId.random),
      buildUserResponse(email = requesterEmail, userId = UserId.random)
    )

    val existingAdminCollaborators = Set(
      Collaborators.Administrator(UserId.random, verifiedAdminEmail),
      Collaborators.Administrator(UserId.random, unverifiedAdminEmail),
      Collaborators.Administrator(UserId.random, requesterEmail)
    )

    val existingCollaborators: Set[Collaborator] = existingAdminCollaborators ++ Set(Collaborator("collaborator1@example.com".toLaxEmail, Roles.DEVELOPER, UserId.random))
    val productionApplication                    = buildApplication().deployedToProduction.withCollaborators(existingCollaborators)

  }

  "non request Type command" should {
    val actor        = Actors.AppCollaborator("someEMail".toLaxEmail)

    "call third party application  with same command as passed in" in new Setup {
      val request = UpdateRedirectUris(actor, List.empty, List.empty, now)

      EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.UpdateApplication.willReturnSuccess(productionApplication)

      val result: Application = await(service.updateApplication(productionApplication, request))

      result shouldBe productionApplication
    }

  }
}
