package org.sdase.commons.server.cloudevents;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.net.URI;

public class CloudEventsBundle<C extends Configuration> implements ConfiguredBundle<C> {

  private ObjectMapper objectMapper;

  public static <C extends Configuration> FinalBuilder<C> builder() {
    return new Builder<>();
  }

  @Override
  public void run(C configuration, Environment environment) {
    this.objectMapper = environment.getObjectMapper();
  }

  @Override
  public void initialize(Bootstrap<?> bootstrap) {
    // do nothing here
  }

  public CloudEventsConsumerHelper createCloudEventsConsumerHelper() {
    return new CloudEventsConsumerHelper(objectMapper);
  }

  @SuppressWarnings("unused")
  public <T> CloudEventsProducerHelper<T> createCloudEventsProducerHelper(
      Class<T> eventType, URI source, String type) {
    return new CloudEventsProducerHelper<>(this.objectMapper, source, type);
  }

  public interface FinalBuilder<C extends Configuration> {

    CloudEventsBundle<C> build();
  }

  public static class Builder<C extends Configuration> implements FinalBuilder<C> {

    private Builder() {}

    @Override
    public CloudEventsBundle<C> build() {
      return new CloudEventsBundle<>();
    }
  }
}
