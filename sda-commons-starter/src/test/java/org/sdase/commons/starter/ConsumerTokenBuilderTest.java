package org.sdase.commons.starter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.dropwizard.core.Configuration;
import io.dropwizard.core.setup.Environment;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sdase.commons.server.consumer.ConsumerTokenBundle;
import org.sdase.commons.server.consumer.filter.ConsumerTokenServerFilter;
import org.sdase.commons.starter.test.BundleAssertion;

class ConsumerTokenBuilderTest {

  private BundleAssertion<SdaPlatformConfiguration> bundleAssertion;

  private Environment environmentMock;
  private ArgumentCaptor<Object> jerseyRegistrationCaptor;

  @BeforeEach
  void setUp() {
    bundleAssertion = new BundleAssertion<>();
    environmentMock = mock(Environment.class, RETURNS_DEEP_STUBS);
    jerseyRegistrationCaptor = ArgumentCaptor.forClass(Object.class);
  }

  @Test
  void withOptionalConsumerTokenByDefault() {
    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .addOpenApiResourcePackageClass(this.getClass())
            .build();

    verifyRegisteredConsumerTokenFiltersEqual(
        bundle, ConsumerTokenBundle.builder().withOptionalConsumerToken().build());
  }

  @Test
  void withRequiredConsumerToken() {
    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .addOpenApiResourcePackageClass(this.getClass())
            .build();

    verifyRegisteredConsumerTokenFiltersEqual(
        bundle, ConsumerTokenBundle.builder().withOptionalConsumerToken().build());
  }

  private void verifyRegisteredConsumerTokenFiltersEqual(
      SdaPlatformBundle<SdaPlatformConfiguration> actualSdaPlatformBundle,
      ConsumerTokenBundle<Configuration> expectedBundle) {
    //noinspection unchecked
    ConsumerTokenBundle<SdaPlatformConfiguration> actualConsumerTokenBundle =
        bundleAssertion.getBundleOfType(actualSdaPlatformBundle, ConsumerTokenBundle.class);

    actualConsumerTokenBundle.run(new SdaPlatformConfiguration(), environmentMock);
    expectedBundle.run(new Configuration(), environmentMock);

    verify(environmentMock.jersey(), times(2)).register(jerseyRegistrationCaptor.capture());

    List<Object> registeredFilters = jerseyRegistrationCaptor.getAllValues();
    var filter1 = (ConsumerTokenServerFilter) registeredFilters.get(0);
    var filter2 = (ConsumerTokenServerFilter) registeredFilters.get(1);
    assertThat(filter1).isEqualTo(filter2);
  }
}
