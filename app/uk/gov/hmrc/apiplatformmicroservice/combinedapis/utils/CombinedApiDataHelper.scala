/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.apiplatformmicroservice.combinedapis.utils

import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiAccessType._
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models._
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.services.FiltersForCombinedApis
import uk.gov.hmrc.apiplatformmicroservice.combinedapis.models.ApiType.{REST_API, XML_API}
import uk.gov.hmrc.apiplatformmicroservice.combinedapis.models.CombinedApi
import uk.gov.hmrc.apiplatformmicroservice.xmlapis.models.XmlApi

object CombinedApiDataHelper extends FiltersForCombinedApis {
  private def determineApiAccessType(api: ApiDefinition): ApiAccessType ={
    if(allVersionsArePublicAccess(api)) PUBLIC else PRIVATE
  }
  private def determineApiAccessType(api: ExtendedApiDefinition): ApiAccessType ={
    if(allVersionsArePublicAccess(api)) PUBLIC else PRIVATE
  }

  def fromApiDefinition(api: ApiDefinition) = CombinedApi(api.name, api.serviceName, api.categories, REST_API, determineApiAccessType(api))
  def fromExtendedApiDefinition(api: ExtendedApiDefinition) = CombinedApi(api.name, api.serviceName, api.categories, REST_API, determineApiAccessType(api))
  def fromXmlApi(api: XmlApi) = CombinedApi(api.name, api.serviceName, api.categories.getOrElse(List.empty), XML_API, PUBLIC)
}
