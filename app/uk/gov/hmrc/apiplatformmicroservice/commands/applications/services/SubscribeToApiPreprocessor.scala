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

import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.Application
import uk.gov.hmrc.http.HeaderCarrier
import scala.concurrent.ExecutionContext
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models._
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.services.ApiDefinitionsForApplicationFetcher
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.EnvironmentAwareSubscriptionFieldsConnector
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.SubscriptionFieldsFetcher
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ApiIdentifier
import uk.gov.hmrc.apiplatform.modules.subscriptions.domain.models.ApiFieldMap
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.ApplicationByIdFetcher
import uk.gov.hmrc.apiplatform.modules.common.services.EitherTHelper
import cats.data.NonEmptyChain
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.services.BaseCommandHandler
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress
import scala.concurrent.Future

@Singleton
class SubscribeToApiPreprocessor @Inject()(
    apiDefinitionsForApplicationFetcher: ApiDefinitionsForApplicationFetcher,
    subscriptionFieldsConnector: EnvironmentAwareSubscriptionFieldsConnector,
    applicationService: ApplicationByIdFetcher,
    subscriptionFieldsFetcher: SubscriptionFieldsFetcher
)(implicit val ec: ExecutionContext)
extends AbstractAppCmdPreprocessor[ApplicationCommands.SubscribeToApi] with BaseCommandHandler[String] {

  private def isPublic(in: ApiVersionDefinition) = in.access match {
    case PublicApiAccess() => true
    case _                 => false
  }

  private def removePrivateVersions(in: Seq[ApiDefinition]): Seq[ApiDefinition] =
    in.map(d => d.copy(versions = d.versions.filter(isPublic))).filterNot(_.versions.isEmpty)
 
  private def canSubscribe(allowedSubscriptions: Seq[ApiDefinition], newSubscriptionApiIdentifier: ApiIdentifier): Boolean = {
    val allVersions: Seq[ApiIdentifier] = allowedSubscriptions.flatMap(api => api.versions.map(version => ApiIdentifier(api.context, version.version)))

    allVersions.contains(newSubscriptionApiIdentifier)
  }
 
  private def isSubscribed(existingSubscriptions: Set[ApiIdentifier], newSubscriptionApiIdentifier: ApiIdentifier): Boolean = {
      existingSubscriptions.contains(newSubscriptionApiIdentifier)
  } 

  // Should be done post subscribe probably but it never has been
  private def createFieldValues(application: Application, apiIdentifier: ApiIdentifier)(implicit hc: HeaderCarrier): Future[Either[NonEmptyChain[CommandFailure],Unit]] = {
    import cats.syntax.either._

    for {
      fieldValues      <- subscriptionFieldsFetcher.fetchFieldValuesWithDefaults(application.deployedTo, application.clientId, Set(apiIdentifier))
      fieldValuesForApi = ApiFieldMap.extractApi(apiIdentifier)(fieldValues)
      fvResults         <- subscriptionFieldsConnector(application.deployedTo).saveFieldValues(application.clientId, apiIdentifier, fieldValuesForApi)
    } yield fvResults.fold(_ => CommandFailures.GenericFailure("Creation of field values failed").leftNec[Unit], _ => ().rightNec[CommandFailure])
  }

  def process(application: Application, cmd: ApplicationCommands.SubscribeToApi, data: Set[LaxEmailAddress])(implicit hc: HeaderCarrier): AppCmdPreprocessorTypes.ResultT = {
    val newSubscriptionApiIdentifier = cmd.apiIdentifier

    for {
      existingSubscriptions <- E.liftF(applicationService.fetchApplicationWithSubscriptionData(application.id).map(_.get.subscriptions)) // .get is safe as we already have the app
      isAlreadySubscribed    = isSubscribed(existingSubscriptions, newSubscriptionApiIdentifier)
      _                      = cond(isAlreadySubscribed, CommandFailures.DuplicateSubscription)
      possibleSubscriptions <- E.liftF(apiDefinitionsForApplicationFetcher.fetch(application, existingSubscriptions, cmd.restricted))
      allowedSubscriptions   = if (cmd.restricted) removePrivateVersions(possibleSubscriptions) else possibleSubscriptions
      notAllowed             = false == canSubscribe(allowedSubscriptions, newSubscriptionApiIdentifier)
      _                      = cond(notAllowed, CommandFailures.SubscriptionNotAvailable)
      _                     <- E.fromEitherF(createFieldValues(application, newSubscriptionApiIdentifier))
    } yield DispatchRequest(cmd, data)
  }
}
