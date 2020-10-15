package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks

import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.ApplicationByIdFetcher
import scala.concurrent.Future
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications.Application
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

trait ApplicationByIdFetcherModule extends MockitoSugar with ArgumentMatchersSugar {

  object ApplicationByIdFetcherMock {
    val aMock = mock[ApplicationByIdFetcher]

    object FetchApplication {

      def willReturnApplication(app: Application) = {
        when(aMock.fetchApplication(*)(*)).thenReturn(Future.successful(Some(app)))
      }

      def willThrowException(e: Exception) = {
        when(aMock.fetchApplication(*)(*)).thenReturn(Future.failed(e))
      }
    }
  }
}
