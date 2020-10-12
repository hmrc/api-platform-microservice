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

package uk.gov.hmrc.apiplatformmicroservice.apidefinition.services

import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models._
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.ApplicationId

trait FilterApis {

  private type ApiFilterFn = ((ApiContext, ApiVersionDefinition)) => Boolean

  private def filterApis(filterFn: ApiFilterFn)(apis: Seq[APIDefinition]): Seq[APIDefinition] = {
    
    def filteredVersions(filterFn: ApiFilterFn)(apiContext: ApiContext, versions: Seq[ApiVersionDefinition]): Seq[ApiVersionDefinition] = {
      versions
        .map(v => ((apiContext, v)) )
        .filter(filterFn)
        .map(_._2)
    }

    apis
      .filterNot(_.requiresTrust)
      .map(api => api.copy(versions = filteredVersions(filterFn)(api.context, api.versions)))
      .filterNot(_.versions.isEmpty)
  }

  // TRY TO REMOVE COMMON versions.map line and COMMON map(_._2) lines into filterApis fn
  def filterApisForDocumentation(applicationIds: Set[ApplicationId], subscriptions: Set[ApiIdentifier])(apis: Seq[APIDefinition]): Seq[APIDefinition] = {
    filterApis(
      Some(_)
      .filterNot(isRetired)
      .filterNot(t => isDeprecated(t) && isNotSubscribed(subscriptions)(t))
      .filter(t => isSubscribed(subscriptions)(t) || isPublicAccess(t) || isPrivateButAllowListed(applicationIds)(t) || isPrivateTrial(t) )
      .isDefined
    )(apis)
  }

  // Not allowing production apps post approval can't be subscribed in DevHub - handled by DevHub

  def filterApisForDevHubSubscription(applicationIds: Set[ApplicationId], subscriptions: Set[ApiIdentifier])(apis: Seq[APIDefinition]): Seq[APIDefinition] = {
    filterApis(
      Some(_)
      .filterNot(isRetired)
      .filterNot(isAlpha)
      .filterNot(t => isDeprecated(t) && isNotSubscribed(subscriptions)(t))
      .filter(t => isSubscribed(subscriptions)(t) || isPublicAccess(t) || isPrivateButAllowListed(applicationIds)(t) )
      .isDefined
    )(apis)
  }

  def filterApisForGateKeeperSubscription(applicationIds: Set[ApplicationId], subscriptions: Set[ApiIdentifier])(apis: Seq[APIDefinition]): Seq[APIDefinition] = {
    filterApis(
      Some(_)
      .filterNot(isRetired)
      .isDefined
    )(apis)
  }

  private val isRetired: ApiFilterFn = t => t._2.status == APIStatus.RETIRED

  private val isDeprecated: ApiFilterFn = t => t._2.status == APIStatus.DEPRECATED

  private val isAlpha: ApiFilterFn = t => t._2.status == APIStatus.ALPHA

  private val isPrivateTrial: ApiFilterFn = t => t._2.access match {
    case PrivateApiAccess(_, true) => true
    case _ => false 
  }

  private val isPublicAccess: ApiFilterFn = t => t._2.access match {
    case PublicApiAccess() => true
    case _                 => false
  }

  private def isPrivateButAllowListed(applicationIds: Set[ApplicationId]): ApiFilterFn = t => {
    t._2.access match {
      case PrivateApiAccess(allowList, _) => allowList.toSet.intersect(applicationIds).headOption.isDefined
      case _                              => false
    }
  }

  private def isSubscribed(subscriptions: Set[ApiIdentifier]): ApiFilterFn = t =>
    subscriptions.contains(ApiIdentifier(t._1, t._2.version))

  private def isNotSubscribed(subscriptions: Set[ApiIdentifier]): ApiFilterFn = t =>
    isSubscribed(subscriptions)(t) == false
}
