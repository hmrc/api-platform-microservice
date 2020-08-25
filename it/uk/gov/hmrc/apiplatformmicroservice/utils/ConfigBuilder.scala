package uk.gov.hmrc.apiplatformmicroservice.utils

import play.api.Configuration

trait ConfigBuilder {
  protected def stubConfig(wiremockHost: String, wiremockPort: Int) = Configuration(
    "microservice.services.api-definition-principal.host" -> wiremockHost,
    "microservice.services.api-definition-principal.port" -> wiremockPort,

    "microservice.services.api-definition-subordinate.host" -> wiremockHost,
    "microservice.services.api-definition-subordinate.port" -> wiremockPort,

    "microservice.services.third-party-application-principal.host" -> wiremockHost,
    "microservice.services.third-party-application-principal.port" -> wiremockPort,

    "microservice.services.third-party-application-subordinate.host" -> wiremockHost,
    "microservice.services.third-party-application-subordinate.port" -> wiremockPort,

    "microservice.services.subscription-fields-principal.host" -> wiremockHost,
    "microservice.services.subscription-fields-principal.port" -> wiremockPort,

    "microservice.services.subscription-fields-subordinate.host" -> wiremockHost,
    "microservice.services.subscription-fields-subordinate.port" -> wiremockPort,
    "metrics.jvm" -> false
  )

}
