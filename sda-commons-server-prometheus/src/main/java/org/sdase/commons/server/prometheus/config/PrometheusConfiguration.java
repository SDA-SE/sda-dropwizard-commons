package org.sdase.commons.server.prometheus.config;

import java.util.List;

public class PrometheusConfiguration {

  private List<Double> requestPercentiles;
  private boolean enableRequestHistogram;
  private Integer requestDigitsOfPrecision;

  public List<Double> getRequestPercentiles() {
    return requestPercentiles;
  }

  public PrometheusConfiguration setRequestPercentiles(List<Double> requestPercentiles) {
    this.requestPercentiles = requestPercentiles;
    return this;
  }

  public boolean isEnableRequestHistogram() {
    return enableRequestHistogram;
  }

  public PrometheusConfiguration setEnableRequestHistogram(boolean enableRequestHistogram) {
    this.enableRequestHistogram = enableRequestHistogram;
    return this;
  }

  public Integer getRequestDigitsOfPrecision() {
    return requestDigitsOfPrecision;
  }

  public PrometheusConfiguration setRequestDigitsOfPrecision(Integer requestDigitsOfPrecision) {
    this.requestDigitsOfPrecision = requestDigitsOfPrecision;
    return this;
  }
}
