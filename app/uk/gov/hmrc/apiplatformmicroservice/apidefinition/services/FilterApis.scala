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

package uk.gov.hmrc.apiplatformmicroservice.apidefinition.services

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models._

trait FilterApis {

  type ApiFilterFn = (ApiContext) => ApiVersions.ApiVersionFilterFn

  def filterApis(filterFn: ApiFilterFn)(apis: List[ApiDefinition]): List[ApiDefinition] = {

    apis
      .filterNot(_.requiresTrust)
      .flatMap(api =>
        api.filterVersions(filterFn(api.context))
      )
  }

  protected val isRetired: ApiVersions.ApiVersionFilterFn = apiVersion => apiVersion.status == ApiStatus.RETIRED

  protected val isDeprecated: ApiVersions.ApiVersionFilterFn = apiVersion => apiVersion.status == ApiStatus.DEPRECATED

  protected val isAlpha: ApiVersions.ApiVersionFilterFn = apiVersion => apiVersion.status == ApiStatus.ALPHA

  protected val isPrivateTrial: ApiVersions.ApiVersionFilterFn = apiVersion =>
    apiVersion.access match {
      case ApiAccess.Private(true) => true
      case _                       => false
    }

  protected val isPublicAccess: ApiVersions.ApiVersionFilterFn = apiVersion => apiVersion.access.isPublic

  protected def isSubscribed(subscriptions: Set[ApiIdentifier]): ApiFilterFn = (apiContext) =>
    (apiVersion) =>
      subscriptions.contains(ApiIdentifier(apiContext, apiVersion.versionNbr))

  protected def isNotSubscribed(subscriptions: Set[ApiIdentifier]): ApiFilterFn = (apiContext) =>
    (apiVersion) =>
      !isSubscribed(subscriptions)(apiContext)(apiVersion)

  def filterOutRetiredVersions(definition: ApiDefinition): Option[ApiDefinition] = {
    definition.filterVersions((v) => v.status != ApiStatus.RETIRED)
  }

  def filterSubscriptions(apiContext: ApiContext, subscriptions: Set[ApiIdentifier]): ApiVersions.ApiVersionFilterFn = (v) =>
    subscriptions.contains(ApiIdentifier(apiContext, v.versionNbr))

}

trait FilterApiDocumentation extends FilterApis {

  def filterApisForDocumentation(subscriptions: Set[ApiIdentifier])(apis: List[ApiDefinition]): List[ApiDefinition] = {
    filterApis(apiContext =>
      Some(_)
        .filterNot(isRetired)
        .filterNot(v => isDeprecated(v) && isNotSubscribed(subscriptions)(apiContext)(v))
        .filter(v => isSubscribed(subscriptions)(apiContext)(v) || isPublicAccess(v) || isPrivateTrial(v))
        .isDefined
    )(apis)
  }
}

trait FilterDevHubSubscriptions extends FilterApis {
  // Not allowing production apps post approval can't be subscribed in DevHub - handled by DevHub

  def filterApisForDevHubSubscriptions(subscriptions: Set[ApiIdentifier])(apis: List[ApiDefinition]): List[ApiDefinition] = {
    filterApis(apiContext =>
      Some(_)
        .filterNot(isRetired)
        .filterNot(isAlpha)
        .filterNot(v => isDeprecated(v) && isNotSubscribed(subscriptions)(apiContext)(v))
        .filter(v => isSubscribed(subscriptions)(apiContext)(v) || isPublicAccess(v))
        .isDefined
    )(apis)
  }
}

trait FilterGateKeeperSubscriptions extends FilterApis {

  def filterApisForGateKeeperSubscriptions(apis: List[ApiDefinition]): List[ApiDefinition] = {
    filterApis(apiContext =>
      Some(_)
        .filterNot(isRetired)
        .isDefined
    )(apis)
  }
}

trait FiltersForCombinedApis extends FilterApis {

  private def isOnlyPublicAccess(v: ExtendedApiVersion): Boolean = {
    (v.productionAvailability, v.sandboxAvailability) match {
      case (Some(prod: ApiAvailability), Some(sand: ApiAvailability)) =>
        prod.access.isPublic && sand.access.isPublic
      case _                                                          => false
    }
  }

  def filterOutRetiredApis(definitions: List[ApiDefinition]): List[ApiDefinition] = {
    definitions.flatMap(filterOutRetiredVersions)
  }

  def allVersionsArePublicAccess(a: ApiDefinition): Boolean         = a.versions.forall(kv => isPublicAccess(kv._2))
  def allVersionsArePublicAccess(a: ExtendedApiDefinition): Boolean = a.versions.forall(v => isOnlyPublicAccess(v))

}
