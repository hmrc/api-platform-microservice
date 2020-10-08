package uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.mocks

import org.mockito.MockitoSugar
import org.mockito.ArgumentMatchersSugar
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.services.SubscriptionFieldsFetcher

trait SubscriptionFieldsFetcherModule {
  self: MockitoSugar with ArgumentMatchersSugar =>
  
  object SubscriptionFieldsFetcherMock {
    val aMock = mock[SubscriptionFieldsFetcher]
  }
}
