package org.sdase.commons.server.prometheus.test;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.core.Configuration;
import org.sdase.commons.server.prometheus.config.PrometheusConfiguration;

public class PrometheusTestConfiguration extends Configuration {

  @JsonProperty private PrometheusConfiguration prometheus = new PrometheusConfiguration();

  public PrometheusConfiguration getPrometheus() {
    return prometheus;
  }
}
