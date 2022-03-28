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

package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services

import scala.concurrent.ExecutionContext
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.EnvironmentAwareSubscriptionFieldsConnector
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models._
import uk.gov.hmrc.apiplatform.modules.subscriptions.domain.models._
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.{Environment, ThreeDMap}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.ClientId
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.fields.FieldDefinition
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.subscriptions.SubscriptionFieldsDomain.ApiFieldMap
import scala.concurrent.Future
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.subscriptions.SubscriptionFieldsDomain.ApiFieldMap

@Singleton
class SubscriptionFieldsFetcher @Inject()(
  subscriptionFieldsConnector: EnvironmentAwareSubscriptionFieldsConnector
  )(implicit ec: ExecutionContext) {

  def fetchFieldValuesWithDefaults(deployedTo: Environment, clientId: ClientId, subscriptions: Set[ApiIdentifier])(implicit hc: HeaderCarrier): Future[ApiFieldMap[FieldValue]] = {

    def filterBySubs[V](data: ApiFieldMap[V]): ApiFieldMap[V] = {
      ThreeDMap.filter( (c: ApiContext, v: ApiVersion, _: FieldName, _: V) => subscriptions.contains(ApiIdentifier(c,v)) )(data)
    }

    def fillFields(fields: ApiFieldMap[FieldValue], defns: ApiFieldMap[FieldDefinition]): ApiFieldMap[FieldValue] = {
      ThreeDMap.map( (c: ApiContext, v: ApiVersion, fn: FieldName, fv: FieldDefinition) => ThreeDMap.get( (c,v,fn) )(fields).getOrElse(FieldValue("")))(defns)
    }

    val connector = subscriptionFieldsConnector(deployedTo)
    
    for {
      definitions <- connector.bulkFetchFieldDefinitions
      subsDefs = filterBySubs(definitions)
      fields <- connector.bulkFetchFieldValues(clientId)
      subsFields = filterBySubs(fields)
      filledFields = fillFields(subsFields, subsDefs)
    } yield filledFields
  }
}
