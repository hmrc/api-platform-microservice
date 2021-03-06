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

package uk.gov.hmrc.apiplatformmicroservice.apidefinition.models

trait OpenAccessRules {
  
  def isPublicAccess(a: APIAccess): Boolean = a match {
    case PublicApiAccess() => true
    case _ => false
  }

  def isOpenAccess(e: Endpoint): Boolean = e.authType == AuthType.NONE
  def isOpenAccess(v: ApiVersionDefinition): Boolean = v.endpoints.toList.forall(e => isOpenAccess(e))
  def isOpenAccess(a: APIDefinition): Boolean = a.versions.forall(v => isOpenAccess(v) & isPublicAccess(v.access))
}
