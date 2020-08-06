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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.subscriptions

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.ClientId

object SubscriptionFieldsDomain {

  case class VersionSubscription(version: APIVersion, subscribed: Boolean)

  case class APISubscription(name: String, serviceName: String, context: String, versions: Seq[VersionSubscription], requiresTrust: Option[Boolean], isTestSupport: Boolean = false)

  case class APISubscriptionStatus(
      name: String,
      serviceName: String,
      context: String,
      apiVersion: APIVersion,
      subscribed: Boolean,
      requiresTrust: Boolean,
      fields: SubscriptionFieldsWrapper,
      isTestSupport: Boolean = false) {

    def canUnsubscribe: Boolean = {
      apiVersion.status != APIStatus.DEPRECATED
    }
  }

  case class APIVersion(version: String, status: APIStatus)

  sealed trait APIStatus extends EnumEntry

  object APIStatus extends Enum[APIStatus] with PlayJsonEnum[APIStatus] {

    val values = findValues

    case object PROTOTYPED extends APIStatus
    case object PUBLISHED extends APIStatus
    case object ALPHA extends APIStatus
    case object BETA extends APIStatus
    case object STABLE extends APIStatus
    case object DEPRECATED extends APIStatus
    case object RETIRED extends APIStatus
  }

  case class SubscriptionFieldDefinition(
      name: String,
      description: String,
      shortDescription: String,
      hint: String,
      `type`: String,
      access: AccessRequirements)

  case class SubscriptionFieldValue(definition: SubscriptionFieldDefinition, value: String)

  case class SubscriptionFieldsWrapper(
      applicationId: ApplicationId,
      clientId: ClientId,
      apiContext: String,
      apiVersion: String,
      fields: Seq[SubscriptionFieldValue])

  type Fields = Map[String, String]

  object Fields {
    val empty = Map.empty[String, String]
  }
}
