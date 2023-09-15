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

package uk.gov.hmrc.apiplatform.modules.subscriptions.domain

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._

package object models {
  type ApiFieldMap[V] = Map[ApiContext, Map[ApiVersionNbr, Map[FieldName, V]]]

  object ApiFieldMap {
    def empty[V]: ApiFieldMap[V] = Map.empty

    def extractApi[V](apiIdentifier: ApiIdentifier)(map: ApiFieldMap[V]): Map[FieldName, V] =
      map
        .getOrElse(apiIdentifier.context, Map.empty)
        .getOrElse(apiIdentifier.version, Map.empty)
  }
}
