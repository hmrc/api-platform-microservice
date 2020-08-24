package uk.gov.hmrc.apiplatformmicroservice.subscriptionfields

import com.github.tomakehurst.wiremock.client.WireMock._

import play.api.http.Status._
import play.api.http._

trait SubscriptionFieldsMock {

  def mockBulkFetchFieldDefintions() {
    stubFor(get(urlEqualTo("/definition"))
      .willReturn(
        aResponse()
          .withBody("""{
                      |"apis": [
                      |    {
                      |        "apiContext": "customs/declarations",
                      |        "apiVersion": "1.0",
                      |        "fieldDefinitions": [
                      |            {
                      |                "name": "callbackUrl",
                      |                "description": "What's your callback URL for declaration submissions?",
                      |                "hint": "This is how we'll notify you when we've processed them. It must include https and port 443",
                      |                "type": "URL",
                      |                "shortDescription": "Callback URL",
                      |                "validation": {
                      |                    "errorMessage": "Callback URL must be a valid URL",
                      |                    "rules": [
                      |                        {
                      |                            "UrlValidationRule": {}
                      |                        }
                      |                    ]
                      |                },
                      |                "access": { "devhub" : { "read": "adminOnly", "write" : "noOne" } }
                      |            },
                      |            {
                      |                "name": "securityToken",
                      |                "description": "What's the value of the HTTP Authorization header we should use to notify you?",
                      |                "hint": "For example: Basic YXNkZnNhZGZzYWRmOlZLdDVOMVhk",
                      |                "type": "SecureToken",
                      |                "shortDescription": "Security Token",
                      |                "validation": {
                      |                    "errorMessage": "Security Token must be alphanumeric",
                      |                    "rules": [
                      |                        {
                      |                            "RegexValidationRule": {
                      |                                "regex": "^[A-Za-z0-9]+$"
                      |                            }
                      |                        }
                      |                    ]
                      |                }
                      |            },
                      |            {
                      |                "name": "authenticatedEori",
                      |                "description": "What's your Economic Operator Registration and Identification (EORI) number?",
                      |                "hint": "This is your EORI that will associate your application with you as a CSP",
                      |                "type": "STRING",
                      |                "shortDescription": "Authenticated Eori",
                      |                "validation": {
                      |                    "errorMessage": "EORI must be 5 to 10 digits long",
                      |                    "rules": [
                      |                        {
                      |                            "RegexValidationRule": {
                      |                                "regex": "^[0-9]{5,10}$"
                      |                            }
                      |                        }
                      |                    ]
                      |                }
                      |            }
                      |        ]
                      |    },
                      |    {
                      |        "apiContext": "hello",
                      |        "apiVersion": "1.0",
                      |        "fieldDefinitions": [
                      |            {
                      |                "name": "helloworldFieldOne",
                      |                "description": "What is your name?",
                      |                "hint": "You could be Arthur, King of the Britons",
                      |                "type": "STRING",
                      |                "shortDescription": "Field 1"
                      |            },
                      |            {
                      |                "name": "helloworldFieldTwo",
                      |                "description": "What is your quest?",
                      |                "hint": "Seeking Holy Grails is a popular pass time",
                      |                "type": "STRING",
                      |                "shortDescription": "Field 2"
                      |            },
                      |            {
                      |                "name": "helloworldFieldThree",
                      |                "description": "What is the airspeed velocity of an unladen swallow?",
                      |                "hint": "African Swallow",
                      |                "type": "STRING",
                      |                "shortDescription": "Field 3"
                      |            }
                      |        ]
                      |    }
                      |]
                      |}""".stripMargin)
          .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
          .withStatus(OK)
      ))
  }

}
