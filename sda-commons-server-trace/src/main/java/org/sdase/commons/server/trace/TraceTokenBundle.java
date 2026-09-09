package org.sdase.commons.server.trace;

import io.dropwizard.core.Configuration;
import io.dropwizard.core.setup.Environment;
import org.sdase.commons.server.trace.filter.TraceTokenServerFilter;

/**
 * @deprecated This class will be replaced by {@link
 *     org.sdase.commons.server.dropwizard.bundles.TraceTokenBundle} when removing the module {@code
 *     sda-commons-server-trace}. To prepare for the upcoming breaking change, update all references
 *     to {@link org.sdase.commons.server.dropwizard.bundles.TraceTokenBundle} and remove direct
 *     dependencies to {@code sda-commons-server-trace}.
 */
@Deprecated(forRemoval = true)
@SuppressWarnings("java:S2176") // intentionally the same name until removed
public class TraceTokenBundle extends org.sdase.commons.server.dropwizard.bundles.TraceTokenBundle {

  private TraceTokenBundle() {}

  @Override
  public void run(Configuration configuration, Environment environment) {
    TraceTokenServerFilter filter = new TraceTokenServerFilter();
    environment.jersey().register(filter);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder
      extends org.sdase.commons.server.dropwizard.bundles.TraceTokenBundle.Builder {

    public TraceTokenBundle build() {
      return new TraceTokenBundle();
    }
  }
}
