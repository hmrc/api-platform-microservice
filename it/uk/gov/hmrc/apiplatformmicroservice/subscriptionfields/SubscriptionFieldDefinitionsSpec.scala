package uk.gov.hrmc.apiplatformmicroservice.subscriptionfields

import play.api.http.Status._
import play.api.libs.ws.WSClient
import play.api.http.HeaderNames._
import play.api.http.MimeTypes._
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.Environment
import play.api.libs.json.Json
import uk.gov.hmrc.apiplatformmicroservice.utils.WireMockSpec
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.Environment.PRODUCTION
import uk.gov.hmrc.apiplatformmicroservice.subscriptionfields.SubscriptionFieldDefinitionsMock

class SubscriptionFieldDefinitionsSpec extends WireMockSpec with SubscriptionFieldDefinitionsMock {

  "WireMock" should {
    val wsClient = app.injector.instanceOf[WSClient]

    "stub get request" in {
      val testingIn: Environment = PRODUCTION

      mockBulkFetchFieldDefintions(PRODUCTION)

      val response = await(wsClient.url(s"$baseUrl/subscription-fields")
        .withQueryStringParameters("environment" -> testingIn.toString())
        .withHttpHeaders(ACCEPT -> JSON)
        .get())

      response.status shouldBe OK
      Json.parse(response.body) shouldBe Json.parse("""
      {
        "hello": {
            "1.0": {
                "helloworldFieldOne": {
                    "name": "helloworldFieldOne",
                    "description": "What is your name?",
                    "hint": "You could be Arthur, King of the Britons",
                    "type": "STRING",
                    "shortDescription": ""
                },
                "helloworldFieldTwo": {
                    "name": "helloworldFieldTwo",
                    "description": "What is your quest?",
                    "hint": "Seeking Holy Grails is a popular pass time",
                    "type": "STRING",
                    "shortDescription": ""
                },
                "helloworldFieldThree": {
                    "name": "helloworldFieldThree",
                    "description": "What is the airspeed velocity of an unladen swallow?",
                    "hint": "African Swallow",
                    "type": "STRING",
                    "shortDescription": ""
                }
            }
        },
        "customs/declarations": {
            "1.0": {
                "callbackUrl": {
                    "name": "callbackUrl",
                    "description": "What's your callback URL for declaration submissions?",
                    "hint": "This is how we'll notify you when we've processed them. It must include https and port 443",
                    "type": "URL",
                    "shortDescription": "",
                    "validation": {
                        "errorMessage": "Callback URL must be a valid URL",
                        "rules": [
                            {
                                "UrlValidationRule": {}
                            }
                        ]
                    },
                    "access": { "devhub": { "read": "adminOnly", "write": "noOne"}}
                },
                "securityToken": {
                    "name": "securityToken",
                    "description": "What's the value of the HTTP Authorization header we should use to notify you?",
                    "hint": "For example: Basic YXNkZnNhZGZzYWRmOlZLdDVOMVhk",
                    "type": "SecureToken",
                    "shortDescription": "",
                    "validation": {
                        "errorMessage": "Security Token must be alphanumeric",
                        "rules": [
                            {
                                "RegexValidationRule": {
                                    "regex": "^[A-Za-z0-9]+$"
                                }
                            }
                        ]
                    }
                },
                "authenticatedEori": {
                    "name": "authenticatedEori",
                    "description": "What's your Economic Operator Registration and Identification (EORI) number?",
                    "hint": "This is your EORI that will associate your application with you as a CSP",
                    "type": "STRING",
                    "shortDescription": "",
                    "validation": {
                        "errorMessage": "EORI must be 5 to 10 digits long",
                        "rules": [
                            {
                                "RegexValidationRule": {
                                    "regex": "^[0-9]{5,10}$"
                                }
                            }
                        ]
                    }
                }
            }
        }
    }""".stripMargin)
    }
  }
}
