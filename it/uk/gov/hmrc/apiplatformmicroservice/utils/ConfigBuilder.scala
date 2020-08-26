package uk.gov.hmrc.apiplatformmicroservice.utils

import play.api.Configuration

trait ConfigBuilder {

  protected def stubConfig(wiremockPrincipalPort: Int, wiremockSubordinatePort: Int) = Configuration(
    "microservice.services.api-definition-principal.port" -> wiremockPrincipalPort,
    "microservice.services.api-definition-subordinate.port" -> wiremockSubordinatePort,
    "microservice.services.third-party-application-principal.port" -> wiremockPrincipalPort,
    "microservice.services.third-party-application-subordinate.port" -> wiremockSubordinatePort,
    "microservice.services.subscription-fields-principal.port" -> wiremockPrincipalPort,
    "microservice.services.subscription-fields-subordinate.port" -> wiremockSubordinatePort,
    "metrics.jvm" -> false
  )

}
