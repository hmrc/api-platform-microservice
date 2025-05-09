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

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import cats.data.NonEmptyList

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.common.domain.models.{Actors, ApiIdentifier, LaxEmailAddress}
import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.AccessType
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationWithCollaborators
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models._
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.services.BaseCommandHandler
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.services.ApiDefinitionsForApplicationFetcher
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.{ApplicationByIdFetcher, SubscriptionFieldsService}

@Singleton
class SubscribeToApiPreprocessor @Inject() (
    apiDefinitionsForApplicationFetcher: ApiDefinitionsForApplicationFetcher,
    applicationService: ApplicationByIdFetcher,
    subscriptionFieldsService: SubscriptionFieldsService
  )(implicit val ec: ExecutionContext
  ) extends AbstractAppCmdPreprocessor[ApplicationCommands.SubscribeToApi] with BaseCommandHandler[String] {

  private def isPublic(in: ApiVersion) = in.access match {
    case ApiAccess.PUBLIC => true
    case _                => false
  }

  private def excludePrivateVersions(in: Seq[ApiDefinition]): Seq[ApiDefinition] =
    in.map(d => d.copy(versions = d.versions.filter { case (_, v) => isPublic(v) })).filterNot(_.versions.isEmpty)

  private def canSubscribe(allowedSubscriptions: Seq[ApiDefinition], newSubscriptionApiIdentifier: ApiIdentifier): Boolean = {
    val allVersions: Seq[ApiIdentifier] = allowedSubscriptions.flatMap(api => api.versions.keySet.map(versionNbr => ApiIdentifier(api.context, versionNbr)))

    allVersions.contains(newSubscriptionApiIdentifier)
  }

  private def isSubscribed(existingSubscriptions: Set[ApiIdentifier], newSubscriptionApiIdentifier: ApiIdentifier): Boolean = {
    existingSubscriptions.contains(newSubscriptionApiIdentifier)
  }

  // TODO Move to TPA/Remove (API-8358)
  // Should be done post subscribe probably but it never has been
  private def createFieldValues(
      application: ApplicationWithCollaborators,
      apiIdentifier: ApiIdentifier
    )(implicit hc: HeaderCarrier
    ): Future[Either[NonEmptyList[CommandFailure], Unit]] = {
    import cats.syntax.either._

    subscriptionFieldsService.createFieldValues(application.clientId, application.deployedTo, apiIdentifier: ApiIdentifier)
      .map(_.fold(_ => CommandFailures.GenericFailure("Creation of field values failed").leftNel[Unit], _ => ().rightNel[CommandFailure]))
  }

  def process(
      application: ApplicationWithCollaborators,
      cmd: ApplicationCommands.SubscribeToApi,
      data: Set[LaxEmailAddress]
    )(implicit hc: HeaderCarrier
    ): AppCmdPreprocessorTypes.AppCmdResultT = {
    val newSubscriptionApiIdentifier = cmd.apiIdentifier

    val requiredGKUser           = List(AccessType.PRIVILEGED, AccessType.ROPC).contains(application.access.accessType)
    val permissionsPassed        = {
      (requiredGKUser, cmd.actor) match {
        case (true, Actors.GatekeeperUser(_)) => true
        case (true, _)                        => false
        case (_, _)                           => true
      }
    }
    val canManagePrivateVersions = cmd.actor match {
      case Actors.GatekeeperUser(_) => true
      case _                        => false
    }

    def not(in: Boolean) = !in

    for {
      _                     <- E.cond(permissionsPassed, (), NonEmptyList.one(CommandFailures.SubscriptionNotAvailable))
      existingSubscriptions <- E.liftF(applicationService.fetchApplicationWithSubscriptionData(application.id).map(_.get.subscriptions)) // .get is safe as we already have the app
      isAlreadySubscribed    = isSubscribed(existingSubscriptions, newSubscriptionApiIdentifier)
      _                     <- E.cond(not(isAlreadySubscribed), (), NonEmptyList.one(CommandFailures.DuplicateSubscription))
      possibleSubscriptions <- E.liftF(apiDefinitionsForApplicationFetcher.fetch(application.deployedTo, existingSubscriptions, !canManagePrivateVersions))
      allowedSubscriptions   = if (canManagePrivateVersions) possibleSubscriptions else excludePrivateVersions(possibleSubscriptions)
      isAllowed              = canSubscribe(allowedSubscriptions, newSubscriptionApiIdentifier)
      _                     <- E.cond(isAllowed, (), NonEmptyList.one(CommandFailures.SubscriptionNotAvailable))
      // TODO Move to TPA/Remove (API-8358)
      _                     <- E.fromEitherF(createFieldValues(application, newSubscriptionApiIdentifier))
    } yield DispatchRequest(cmd, data)
  }
}
