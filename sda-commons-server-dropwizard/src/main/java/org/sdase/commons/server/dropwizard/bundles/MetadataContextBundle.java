package org.sdase.commons.server.dropwizard.bundles;

import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Environment;
import org.sdase.commons.server.dropwizard.metadata.MetadataContext;
import org.sdase.commons.server.dropwizard.metadata.MetadataContextFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@code MetadataContextBundle} activates handling of the {@link
 * org.sdase.commons.server.dropwizard.metadata.MetadataContext}. The property or environment
 * variable {@code }
 */
public class MetadataContextBundle implements ConfiguredBundle<Configuration> {

  private static final Logger LOG = LoggerFactory.getLogger(MetadataContextBundle.class);

  @Override
  public void run(Configuration configuration, Environment environment) throws Exception {
    var metadataFields = MetadataContext.metadataFields();
    if (!metadataFields.isEmpty()) {
      LOG.info(
          "Metadata Context configured to use {} fields: {}",
          metadataFields.size(),
          metadataFields);
      environment.jersey().register(new MetadataContextFilter(metadataFields));
    } else {
      LOG.info("No fields configured for the Metadata Context.");
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    public MetadataContextBundle build() {
      return new MetadataContextBundle();
    }
  }
}
