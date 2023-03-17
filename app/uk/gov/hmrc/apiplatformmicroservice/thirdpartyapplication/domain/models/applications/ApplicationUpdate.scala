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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications

import java.time.LocalDateTime

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models.Actor

sealed trait ApplicationUpdate {
  def timestamp: LocalDateTime

}
trait UpdateRequest extends ApplicationUpdate

case class SubscribeToApi(actor: Actor, apiIdentifier: ApiIdentifier, timestamp: LocalDateTime)                                     extends ApplicationUpdate
case class UnsubscribeFromApi(actor: Actor, apiIdentifier: ApiIdentifier, timestamp: LocalDateTime)                                 extends ApplicationUpdate
case class UpdateRedirectUris(actor: Actor, oldRedirectUris: List[String], newRedirectUris: List[String], timestamp: LocalDateTime) extends ApplicationUpdate

trait ApplicationUpdateFormatters {
  import play.api.libs.json.Json
  import uk.gov.hmrc.play.json.Union

  implicit val subscribeToApiFormatter               = Json.format[SubscribeToApi]
  implicit val unsubscribeFromApiFormatter           = Json.format[UnsubscribeFromApi]
  implicit val updateRedirectUrisFormatter           = Json.format[UpdateRedirectUris]

  implicit val applicationUpdateFormatter = Union.from[ApplicationUpdate]("updateType")
    .and[SubscribeToApi]("subscribeToApi")
    .and[UnsubscribeFromApi]("unsubscribeFromApi")
    .and[UpdateRedirectUris]("updateRedirectUris")
    .format

}

object ApplicationUpdateFormatters extends ApplicationUpdateFormatters