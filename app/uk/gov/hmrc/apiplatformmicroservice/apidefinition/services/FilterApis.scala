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

  private def filterVersions(applicationId: ApplicationId, subscriptions: Set[ApiIdentifier])(api: APIDefinition): Option[APIDefinition] = {
    def isRetired(version: ApiVersionDefinition): Boolean = version.status == APIStatus.RETIRED

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

    def isDeprecated(versionDefinition: ApiVersionDefinition) = 
      versionDefinition.status == APIStatus.DEPRECATED

    def isAlpha(versionDefinition: ApiVersionDefinition) = 
      versionDefinition.status == APIStatus.ALPHA

    val filteredVersions = canISeeThisApiOnSubscriptionsPage

    // Not allowing production apps post approval can't be subscribed in DevHub - handled by DevHub

    val canISeeThisApiVersionInGateKeeperOnSubscriptionsPage = api.versions
      .filterNot(v => isRetired(v))
      // This is ignored because you may need to be subscribed to see alpha docs (private apis for example)
      // .filterNot(v => isAlpha(v))
      // This is ingored becuase you may need to correct a user error where they unsubscribed by mistake
      // .filterNot(v => isDeprecated(v))

      
    val canISeeTheDocsInDevHub = api.versions
      .filterNot(v => isRetired(v))
      .filterNot(v => isDeprecated(v) && isSubscribed(api.context,v) == false )
      .filter(v => isSubscribed(api.context, v) || isPublicAccess(v) || isPrivateButAllowListed(api.context, v) )

    val canISeeThisApiVersionInDevHubApiOnSubscriptionsPage = canISeeTheDocs  
      .filterNot(v => isAlpha(v)) // You may be subscribed, but you cant see it on the subs page.
    




    def x = canGKSubscribeTheAppToThisApi || canISeeTheDocs || canDevHubSubscribeTheAppToThisApi

      if (filteredVersions.isEmpty) None
    else Some(api.copy(versions = filteredVersions))
  }
}
