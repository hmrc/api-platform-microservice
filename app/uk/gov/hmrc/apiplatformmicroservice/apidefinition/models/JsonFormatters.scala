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

import cats.data.{NonEmptyList => NEL}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.APIAccessType.{PRIVATE, PUBLIC}
import uk.gov.hmrc.apiplatformmicroservice.common.domain.services.{CommonJsonFormatters, NonEmptyListFormatters}

trait BasicApiDefinitionJsonFormatters extends CommonJsonFormatters {
  implicit val formatApiContext: Format[ApiContext] = Json.valueFormat[ApiContext]
  implicit val formatApiVersion: Format[ApiVersion] = Json.valueFormat[ApiVersion]
  implicit val formatApiCategory: Format[APICategory] = Json.valueFormat[APICategory]
  implicit val formatApiCategoryDetails: Format[APICategoryDetails] = Json.format[APICategoryDetails]
  implicit val formatApiIdentifier: Format[ApiIdentifier] = Json.format[ApiIdentifier]

  implicit val keyReadsApiContext: KeyReads[ApiContext] = key => JsSuccess(ApiContext(key))
  implicit val keyWritesApiContext: KeyWrites[ApiContext] = _.value

  implicit val keyReadsApiVersion: KeyReads[ApiVersion] = key => JsSuccess(ApiVersion(key))
  implicit val keyWritesApiVersion: KeyWrites[ApiVersion] = _.value
}

object BasicApiDefinitionJsonFormatters extends BasicApiDefinitionJsonFormatters

trait EndpointJsonFormatters extends NonEmptyListFormatters {
  implicit val formatParameter = Json.format[Parameter]

  implicit val endpointReads: Reads[Endpoint] = (
    (JsPath \ "endpointName").read[String] and
      (JsPath \ "uriPattern").read[String] and
      (JsPath \ "method").read[HttpMethod] and
      (JsPath \ "authType").read[AuthType] and
      ((JsPath \ "queryParameters").read[List[Parameter]] or Reads.pure(List.empty[Parameter]))
  )(Endpoint.apply _)

  implicit val endpointWrites: Writes[Endpoint] = Json.writes[Endpoint]
}

trait ApiDefinitionJsonFormatters extends EndpointJsonFormatters with BasicApiDefinitionJsonFormatters with CommonJsonFormatters {
  import uk.gov.hmrc.apiplatformmicroservice.common.domain.models._

  implicit val apiAccessReads: Reads[APIAccess] = (
    (JsPath \ "type").read[APIAccessType] and
      ((JsPath \ "allowlistedApplicationIds").read[List[ApplicationId]] or Reads.pure(List.empty[ApplicationId])) and
      ((JsPath \ "isTrial").read[Boolean] or Reads.pure(false)) tupled
  ) map {
    case (PUBLIC, _, _)                                => PublicApiAccess()
    case (PRIVATE, allowlistedApplicationIds, isTrial) => PrivateApiAccess(allowlistedApplicationIds, isTrial)
  }

  implicit object apiAccessWrites extends Writes[APIAccess] {

    private val privApiWrites: OWrites[(APIAccessType, List[ApplicationId], Boolean)] = (
      (JsPath \ "type").write[APIAccessType] and
        (JsPath \ "allowlistedApplicationIds").write[List[ApplicationId]] and
        (JsPath \ "isTrial").write[Boolean]
    ).tupled

    override def writes(access: APIAccess) = access match {
      case _: PublicApiAccess           => Json.obj("type" -> PUBLIC)
      case privateApi: PrivateApiAccess => privApiWrites.writes((PRIVATE, privateApi.allowlistedApplicationIds, privateApi.isTrial))
    }
  }

  implicit val apiVersionReads: Reads[ApiVersionDefinition] =
    ((JsPath \ "version").read[ApiVersion] and
      (JsPath \ "status").read[APIStatus] and
      (JsPath \ "access").readNullable[APIAccess] and
      (JsPath \ "endpoints").read[NEL[Endpoint]] and
      ((JsPath \ "endpointsEnabled").read[Boolean] or Reads.pure(false)) tupled) map {
      case (version, status, None, endpoints, endpointsEnabled)         => ApiVersionDefinition(version, status, PublicApiAccess(), endpoints, endpointsEnabled)
      case (version, status, Some(access), endpoints, endpointsEnabled) => ApiVersionDefinition(version, status, access, endpoints, endpointsEnabled)
    }

  implicit val apiVersionWrites: Writes[ApiVersionDefinition] = Json.writes[ApiVersionDefinition]

  implicit val apiDefinitionReads: Reads[APIDefinition] = (
    (JsPath \ "serviceName").read[String] and
      (JsPath \ "name").read[String] and
      (JsPath \ "description").read[String] and
      (JsPath \ "context").read[ApiContext] and
      ((JsPath \ "requiresTrust").read[Boolean] or Reads.pure(false)) and
      ((JsPath \ "isTestSupport").read[Boolean] or Reads.pure(false)) and
      (JsPath \ "versions").read[List[ApiVersionDefinition]] and
      ((JsPath \ "categories").read[List[APICategory]] or Reads.pure(List.empty[APICategory]))
  )(APIDefinition.apply _)

  implicit val apiDefinitionWrites: Writes[APIDefinition] = Json.writes[APIDefinition]

  implicit val formatApiAvailability = Json.format[APIAvailability]
  implicit val formatExtendedApiVersion = Json.format[ExtendedAPIVersion]
  implicit val formatExtendedApiDefinition = Json.format[ExtendedAPIDefinition]
}

object ApiDefinitionJsonFormatters extends ApiDefinitionJsonFormatters
