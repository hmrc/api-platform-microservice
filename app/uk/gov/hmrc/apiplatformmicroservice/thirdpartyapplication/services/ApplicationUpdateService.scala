package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services

import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.connectors.{AbstractThirdPartyApplicationConnector, EnvironmentAwareThirdPartyApplicationConnector}
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.{Application, ApplicationUpdate, SubscribeToApi}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApplicationUpdateService @Inject()(val thirdPartyApplicationConnector: EnvironmentAwareThirdPartyApplicationConnector,
                                         subscriptionService: SubscriptionService) {
  def updateApplication(app: Application, applicationUpdate: ApplicationUpdate)(implicit hc: HeaderCarrier, ec: ExecutionContext) = {

    def callTpa(applicationUpdate: ApplicationUpdate): Future[Application] ={
      thirdPartyApplicationConnector(app.deployedTo).updateApplication(app.id, applicationUpdate)
    }

    for{
      updateCommand <- Future.successful(validateUpdate(applicationUpdate))
      result <- callTpa(updateCommand)
    } yield result


  }

  private def validateUpdate(applicationUpdate: ApplicationUpdate): ApplicationUpdate ={
    applicationUpdate match {
      case _ => _
    }
  }

}
