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

  def filterApis(applicationId: ApplicationId, subscriptions: Set[ApiIdentifier])(apis: Seq[
  APIDefinition]): Seq[
  APIDefinition] = {
    apis.filterNot(_.requiresTrust).flatMap(filterVersions(applicationId, subscriptions))
  }

  private def filterVersions(applicationId: ApplicationId, subscriptions: Set[ApiIdentifier])(api: 
  APIDefinition): Option[
  APIDefinition] = {
    def retiredVersions(version: ApiVersionDefinition): Boolean = version.status == APIStatus.RETIRED

    def isSubscribed(context: ApiContext, versionDefinition: ApiVersionDefinition): Boolean = 
      subscriptions.contains(ApiIdentifier(context, versionDefinition.version))
    
    def isPublicAccess(versionDefinition: ApiVersionDefinition): Boolean = 
      versionDefinition.access match {
        case PublicApiAccess() => true
        case _                 => false
      }

    def isPrivateButAllowListed(context: ApiContext, versionDefinition: ApiVersionDefinition) = 
      versionDefinition.access match {
        case PrivateApiAccess(allowList, _) if allowList.contains(applicationId) => true
        case _                                                                    => false
      }

    def deprecatedAndNotSubscribed(context: ApiContext, versionDefinition: ApiVersionDefinition) = 
      versionDefinition.status == APIStatus.DEPRECATED && !isSubscribed(context, versionDefinition)

    def alphaAndNotSubscribed(context: ApiContext, versionDefinition: ApiVersionDefinition) = 
      versionDefinition.status == APIStatus.ALPHA && !isSubscribed(context, versionDefinition)

    val filteredVersions = api.versions
      .filterNot(v => 
        retiredVersions(v) || 
        deprecatedAndNotSubscribed(api.context, v) ||  // Probably need to allow GK to see this
        alphaAndNotSubscribed(api.context, v)
      )
      .filter(v => 
        isPublicAccess(v) ||
        isPrivateButAllowListed(api.context, v) ||
        isSubscribed(api.context, v)
      )

    if (filteredVersions.isEmpty) None
    else Some(api.copy(versions = filteredVersions))
  }
}
