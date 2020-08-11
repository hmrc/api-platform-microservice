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

package uk.gov.hmrc.apiplatformmicroservice.common

import play.api.libs.ws.WSResponse
import play.api.mvc.Result
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ResourceId
import uk.gov.hmrc.http.NotFoundException

import scala.concurrent.Future

trait StreamedResponseResourceHelper extends StreamedResponseHelper {

  def handler(resourceId: ResourceId): WSResponse => Result = {
    import resourceId._
    streamedResponseAsResult(
      handleNotFoundResponse(s"$resource not found for $serviceName ${version.value}")
        orElse handleErrorsAsInternalServerError(
          s"Error downloading $resource for $serviceName ${version.value}"
        )
    )(_)
  }

  def failedDueToNotFoundException(resourceId: ResourceId): Future[Nothing] = {
    import resourceId._
    Future.failed(
      new NotFoundException(s"$resource not found for $serviceName ${version.value}")
    )
  }
}
