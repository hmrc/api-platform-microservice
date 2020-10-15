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

package uk.gov.hmrc.apiplatformmicroservice.common.controllers

import cats.data.OptionT
import play.api.mvc._
import uk.gov.hmrc.apiplatformmicroservice.common.controllers.domain.{ApplicationRequest, ApplicationWithSubscriptionDataRequest}
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.ApplicationByIdFetcher
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

trait ActionBuilders {
  self: BackendController =>

  val applicationService: ApplicationByIdFetcher

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

}
