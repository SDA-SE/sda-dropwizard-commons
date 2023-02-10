package org.sdase.commons.server.dropwizard.bundles;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.setup.Environment;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearSystemProperty;
import org.junitpioneer.jupiter.SetSystemProperty;
import org.sdase.commons.server.dropwizard.metadata.MetadataContextFilter;

class MetadataContextBundleTest {

  @Test
  @SetSystemProperty(key = "METADATA_FIELDS", value = "tenant-id")
  void shouldRegisterFilterWhenFieldsAreConfigured() throws Exception {
    var metadataContextBundle = new MetadataContextBundle();

    var jerseyEnvironmentMock = mock(JerseyEnvironment.class);
    var environmentMock = mock(Environment.class);
    when(environmentMock.jersey()).thenReturn(jerseyEnvironmentMock);

    metadataContextBundle.run(null, environmentMock);

    verify(jerseyEnvironmentMock, times(1)).register(any(MetadataContextFilter.class));
  }

  @Test
  @ClearSystemProperty(key = "METADATA_FIELDS")
  void shouldNotRegisterFilterWhenFieldsAreNotConfigured() throws Exception {
    var metadataContextBundle = new MetadataContextBundle();

    var environmentMock = mock(Environment.class);

    metadataContextBundle.run(null, environmentMock);

    verifyNoInteractions(environmentMock);
  }

  @Test
  @SetSystemProperty(key = "METADATA_FIELDS", value = ", , ,")
  void shouldNotRegisterFilterWhenFieldsAreEmpty() throws Exception {
    var metadataContextBundle = new MetadataContextBundle();

    var environmentMock = mock(Environment.class);

    metadataContextBundle.run(null, environmentMock);

    verifyNoInteractions(environmentMock);
  }
}
