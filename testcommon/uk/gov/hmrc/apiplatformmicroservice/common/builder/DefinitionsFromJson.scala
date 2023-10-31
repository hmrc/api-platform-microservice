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

package uk.gov.hmrc.apiplatformmicroservice.common.builder

import play.api.libs.json.Json

import uk.gov.hmrc.apiplatform.modules.apis.domain.models.{ApiDefinition, ExtendedApiDefinition}

trait DefinitionsFromJson {

  // noinspection ScalaStyle
  def extendedApiDefinition(name: String): ExtendedApiDefinition = {
    Json.parse(s"""{
                  |  "name" : "$name",
                  |  "description" : "Test API",
                  |  "context" : "test",
                  |  "serviceBaseUrl" : "http://test",
                  |  "serviceName" : "test",
                  |  "requiresTrust": false,
                  |  "isTestSupport": false,
                  |  "versions" : [
                  |    {
                  |      "version" : "1.0",
                  |      "status" : "STABLE",
                  |      "endpointsEnabled": true,
                  |      "endpoints" : [
                  |        {
                  |          "uriPattern" : "/hello",
                  |          "endpointName" : "Say Hello",
                  |          "method" : "GET",
                  |          "authType" : "NONE",
                  |          "throttlingTier" : "UNLIMITED",
                  |          "queryParameters": []
                  |        }
                  |      ],
                  |      "productionAvailability": {
                  |        "endpointsEnabled": true,
                  |        "access": {
                  |          "type": "PUBLIC"
                  |        },
                  |        "loggedIn": false,
                  |        "authorised": true
                  |      }
                  |    },
                  |    {
                  |      "version" : "2.0",
                  |      "status" : "STABLE",
                  |      "endpoints" : [
                  |        {
                  |          "uriPattern" : "/hello",
                  |          "endpointName" : "Say Hello",
                  |          "method" : "GET",
                  |          "authType" : "NONE",
                  |          "throttlingTier" : "UNLIMITED",
                  |          "scope": "read:hello"
                  |        }
                  |      ],
                  |      "productionAvailability": {
                  |        "endpointsEnabled": true,
                  |        "access": {
                  |          "type": "PRIVATE"
                  |        },
                  |        "loggedIn": false,
                  |        "authorised": false
                  |      }
                  |    }
                  |  ]
                  |}
     """.stripMargin).as[ExtendedApiDefinition]
  }

  def apiDefinition(name: String): ApiDefinition = {
    Json.parse(s"""{
                  |  "name" : "$name",
                  |  "description" : "Test API",
                  |  "context" : "test",
                  |  "serviceBaseUrl" : "http://test",
                  |  "serviceName" : "test",
                  |  "isTestSupport": false,
                  |  "requiresTrust": false,
                  |  "categories": [ "OTHER" ],
                  |  "versions" : [
                  |    {
                  |      "version" : "1.0",
                  |      "status" : "STABLE",
                  |      "access": { "type": "PUBLIC" },
                  |      "endpointsEnabled": true,
                  |      "versionSource": "UNKNOWN",
                  |      "endpoints" : [
                  |        {
                  |          "uriPattern" : "/hello",
                  |          "endpointName" : "Say Hello",
                  |          "method" : "GET",
                  |          "authType" : "NONE",
                  |          "throttlingTier" : "UNLIMITED",
                  |          "queryParameters": []
                  |        }
                  |      ]
                  |    },
                  |    {
                  |      "version" : "2.0",
                  |      "status" : "STABLE",
                  |      "access": { "type": "PUBLIC" },
                  |      "endpointsEnabled": true,
                  |      "versionSource": "UNKNOWN",
                  |      "endpoints" : [
                  |        {
                  |          "uriPattern" : "/hello",
                  |          "endpointName" : "Say Hello",
                  |          "method" : "GET",
                  |          "authType" : "NONE",
                  |          "throttlingTier" : "UNLIMITED",
                  |          "scope": "read:hello",
                  |          "queryParameters": []
                  |        }
                  |      ]
                  |    }
                  |  ]
                  |}""".stripMargin.replaceAll("\n", " ")).as[ApiDefinition]
  }
  def apiDefinitions(names: String*)             = names.map(apiDefinition)
}
