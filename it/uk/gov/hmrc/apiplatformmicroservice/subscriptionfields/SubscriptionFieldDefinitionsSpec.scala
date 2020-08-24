package uk.gov.hrmc.apiplatformmicroservice.subscriptionfields

import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import play.api.http.Status._
import org.mockito.MockitoSugar
import org.mockito.ArgumentMatchersSugar
import org.scalatest.OptionValues
import org.scalatestplus.play.WsScalaTestClient
import play.api.test.DefaultAwaitTimeout
import play.api.test.FutureAwaits
import play.api.Configuration
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.Mode
import uk.gov.hmrc.apiplatformmicroservice.subscriptionfields.SubscriptionFieldsMock
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.ws.WSClient
import play.api.http.HeaderNames._
import play.api.http.MimeTypes._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.Environment
import play.api.libs.json.Json

class SubscriptionFieldDefinitionsSpec
    extends WordSpec
    with Matchers
    with OptionValues
    with WsScalaTestClient
    with MockitoSugar
    with ArgumentMatchersSugar
    with DefaultAwaitTimeout
    with FutureAwaits
    with BeforeAndAfterEach
    with GuiceOneServerPerSuite
    with SubscriptionFieldsMock {

  val WireMockHost = "localhost"
  val WireMockPort = 11111
  val wireMockServer = new WireMockServer(wireMockConfig().port(WireMockPort))
  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val stubConfig = Configuration(
    "microservice.services.api-definition-principal.host" -> WireMockHost,
    "microservice.services.api-definition-subordinate.host" -> WireMockHost,
    "microservice.services.third-party-application-principal.host" -> WireMockHost,
    "microservice.services.third-party-application-subordinate.host" -> WireMockHost,
    "microservice.services.subscription-fields-principal.host" -> WireMockHost,
    "microservice.services.subscription-fields-subordinate.host" -> WireMockHost,
    "microservice.services.api-definition-principal.port" -> WireMockPort,
    "microservice.services.api-definition-subordinate.port" -> WireMockPort,
    "microservice.services.third-party-application-principal.port" -> WireMockPort,
    "microservice.services.third-party-application-subordinate.port" -> WireMockPort,
    "microservice.services.subscription-fields-principal.port" -> WireMockPort,
    "microservice.services.subscription-fields-subordinate.port" -> WireMockPort,
    "metrics.jvm" -> false
  )

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(stubConfig)
      .in(Mode.Test)
      .build()

  override lazy val port = 8080

  lazy val baseUrl = s"http://localhost:$port"

  override def beforeEach {
    wireMockServer.start()
    WireMock.configureFor(WireMockHost, WireMockPort)
  }

  override def afterEach {
    wireMockServer.stop()
  }

  "WireMock" should {
    val wsClient = app.injector.instanceOf[WSClient]

    "stub get request" in {
      mockBulkFetchFieldDefintions()
      val response = await(wsClient.url(s"$baseUrl/subscription-fields")
        .withQueryStringParameters("environment" -> Environment.PRODUCTION.toString())
        .withHttpHeaders(ACCEPT -> JSON)
        .get())

      println(response.body)
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
