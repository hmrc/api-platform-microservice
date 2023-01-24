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

import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ApiVersion
import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.ApiDefinitionTestDataHelper
import uk.gov.hmrc.apiplatformmicroservice.common.utils.HmrcSpec

class CdsVersionHandlerSpec extends HmrcSpec with ApiDefinitionTestDataHelper {
  import CdsVersionHandler.specialCaseContexts

  val uninterestingApi = "uninteresting".asIdentifier()

  val v1 = "customs/declarations".asIdentifier()
  val v2 = "customs/declarations".asIdentifier(ApiVersion("2.0"))

  "CdsVersionHandler" should {
    "populateSpecialCases" can {
      "add version two to the inbound set when one of the cds contexts exists in the input" in {
        val inboundApis = Set(uninterestingApi, v1)

        val updatedApis = CdsVersionHandler.populateSpecialCases(inboundApis)

        updatedApis should contain only (uninterestingApi, v1, v2)
      }

      "add nothing if the inbound set contains version three references" in {
        val doesNotCount = "customs/declarations".asIdentifier(ApiVersion("3.0"))
        val inboundApis  = Set(uninterestingApi, doesNotCount)

        val updatedApis = CdsVersionHandler.populateSpecialCases(inboundApis)

        updatedApis shouldBe inboundApis
      }

      "add nothing if the inbound set contains none of the cds contexts" in {
        val inboundApis = Set(uninterestingApi)

        val updatedApis = CdsVersionHandler.populateSpecialCases(inboundApis)

        updatedApis shouldBe inboundApis
      }

      "add version two without ending up with duplicates even if the inbound set contains the same version" in {
        val inboundApis = Set(uninterestingApi, v1, v2)

        val updatedApis = CdsVersionHandler.populateSpecialCases(inboundApis)

        updatedApis.size shouldBe 3
        updatedApis should contain only (uninterestingApi, v1, v2)
      }

      "add version two for all CDS contexts if they exist in the input" in {
        val entries     = specialCaseContexts.size
        val inboundApis = specialCaseContexts.map(_.asIdentifier) + uninterestingApi

        val updatedApis = CdsVersionHandler.populateSpecialCases(inboundApis)

        updatedApis.size shouldBe entries * 2 + 1

        updatedApis should contain(uninterestingApi)
        updatedApis.intersect(specialCaseContexts.map(_.asIdentifier)).size shouldBe entries
        updatedApis.intersect(specialCaseContexts.map(_.asIdentifier(ApiVersion("2.0")))).size shouldBe entries
      }
    }

    "adjustSpecialCaseVersions" can {
      "leave the set unaffected if none of the CDS contexts are present" in {
        val inboundApis = Set(uninterestingApi)

        val updatedApis = CdsVersionHandler.adjustSpecialCaseVersions(inboundApis)

        updatedApis should contain only (uninterestingApi)
      }

      "change version 2 to version 1 for CDS contexts" in {
        val inboundApis = Set(uninterestingApi, v2)

        val updatedApis = CdsVersionHandler.adjustSpecialCaseVersions(inboundApis)

        updatedApis should contain only (uninterestingApi, v1)
      }

      "change all version 2 to version 1 for CDS contexts" in {
        val entries     = specialCaseContexts.size
        val inboundApis = specialCaseContexts.map(_.asIdentifier(ApiVersion("2.0"))) + uninterestingApi

        val updatedApis = CdsVersionHandler.adjustSpecialCaseVersions(inboundApis)

        updatedApis.size shouldBe entries + 1

        updatedApis should contain(uninterestingApi)
        updatedApis.intersect(specialCaseContexts.map(_.asIdentifier)).size shouldBe entries
        updatedApis.intersect(specialCaseContexts.map(_.asIdentifier(ApiVersion("2.0")))).size shouldBe 0
      }

      "do not change version 3 to version 1 for CDS contexts" in {
        val inboundApis = specialCaseContexts.map(_.asIdentifier(ApiVersion("3.0"))) + uninterestingApi

        val updatedApis = CdsVersionHandler.adjustSpecialCaseVersions(inboundApis)

        updatedApis shouldBe inboundApis
      }
    }
  }
}
