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

package uk.gov.hmrc.apiplatformmicroservice.common.controllers

import cats.data.OptionT
import play.api.mvc._
import uk.gov.hmrc.apiplatformmicroservice.common.controllers.domain.{ApplicationRequest, ApplicationWithSubscriptionDataRequest}
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.ApplicationByIdFetcher
import uk.gov.hmrc.play.bootstrap.controller.BackendController
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.AccessType

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.Future.successful
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.AccessType.PRIVILEGED
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.AccessType.ROPC
import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.auth.core.retrieve.EmptyRetrieval
import uk.gov.hmrc.apiplatformmicroservice.common.connectors.AuthConnector
import uk.gov.hmrc.play.HeaderCarrierConverter

trait ActionBuilders {
  self: BackendController =>

  implicit def ec: ExecutionContext

  val applicationService: ApplicationByIdFetcher
  val authConfig: AuthConnector.Config
  val authConnector: AuthConnector

  private def applicationRefiner(applicationId: ApplicationId)(implicit ec: ExecutionContext): ActionRefiner[Request, ApplicationRequest] =
    new ActionRefiner[Request, ApplicationRequest] {
      override protected def executionContext: ExecutionContext = ec

      override def refine[A](request: Request[A]): Future[Either[Result, ApplicationRequest[A]]] = {
        implicit val r: Request[A] = request
        import cats.implicits._

        (for {
          application <- OptionT(applicationService.fetchApplication(applicationId))
        } yield {
          ApplicationRequest(application, application.deployedTo, request)
        }).toRight(NotFound).value
      }
    }

  def ApplicationAction(applicationId: ApplicationId)(implicit ec: ExecutionContext): ActionBuilder[ApplicationRequest, AnyContent] =
    Action.andThen(applicationRefiner(applicationId))

  private def applicationWithSubscriptionDataRefiner(applicationId: ApplicationId)(implicit ec: ExecutionContext): ActionRefiner[Request, ApplicationWithSubscriptionDataRequest] =
    new ActionRefiner[Request, ApplicationWithSubscriptionDataRequest] {
      override protected def executionContext: ExecutionContext = ec

      override def refine[A](request: Request[A]): Future[Either[Result, ApplicationWithSubscriptionDataRequest[A]]] = {
        implicit val r: Request[A] = request
        import cats.implicits._

        (for {
          applicationWithSubscriptionData <- OptionT(applicationService.fetchApplicationWithSubscriptionData(applicationId))
        } yield {
          ApplicationWithSubscriptionDataRequest(
            applicationWithSubscriptionData.application,
            applicationWithSubscriptionData.subscriptions,
            applicationWithSubscriptionData.application.deployedTo,
            request
          )
        }).toRight(NotFound).value
      }
    }

  def ApplicationWithSubscriptionDataAction(applicationId: ApplicationId)(implicit ec: ExecutionContext): ActionBuilder[ApplicationWithSubscriptionDataRequest, AnyContent] =
    Action andThen applicationWithSubscriptionDataRefiner(applicationId)

  def RequiresAuthenticationForPrivilegedOrRopcApplications(applicationId: ApplicationId)(implicit ec: ExecutionContext): ActionBuilder[ApplicationWithSubscriptionDataRequest, AnyContent] =
    ApplicationWithSubscriptionDataAction(applicationId) andThen RepositoryBasedApplicationTypeFilter(applicationId, List(PRIVILEGED, ROPC), false)

  private case class RepositoryBasedApplicationTypeFilter(applicationId: ApplicationId, toMatchAccessTypes: List[AccessType], failOnAccessTypeMismatch: Boolean)
                                                          (implicit ec: ExecutionContext) extends ActionFilter[ApplicationWithSubscriptionDataRequest] {
    protected def executionContext: ExecutionContext = ec

    lazy val FAILED_ACCESS_TYPE = successful(Some(Results.Forbidden(JsErrorResponse(ErrorCode.APPLICATION_NOT_FOUND, "application access type mismatch"))))

    def filter[A](request: ApplicationWithSubscriptionDataRequest[A]): Future[Option[Result]] =
      if(toMatchAccessTypes.contains(request.application.access.accessType)) authenticate(request.request)
      else if(failOnAccessTypeMismatch) FAILED_ACCESS_TYPE else successful(None)
  }

  private def authenticate[A](input: Request[A]): Future[Option[Result]] = {
    if (authConfig.enabled) {
      implicit val hc = HeaderCarrierConverter.fromHeadersAndSession(input.headers, None)
      val hasAnyGatekeeperEnrolment = Enrolment(authConfig.userRole) or Enrolment(authConfig.superUserRole) or Enrolment(authConfig.adminRole)
      authConnector.authorise(hasAnyGatekeeperEnrolment, EmptyRetrieval).map { _ => None }
    } else {
      Future.successful(None)
    }
  }
}
