package org.sdase.commons.server.kafka.helper;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper Class to save a list of metrics. Can be used by {@link KafkaMetricsReporter} and unit
 * tests.
 */
public class MetricsHelper {

  private static List<String> listOfMetrics = new ArrayList<>();

  public static List<String> getListOfMetrics() {
    return listOfMetrics;
  }

  public static void addMetric(String name) {
    listOfMetrics.add(name);
  }
}
