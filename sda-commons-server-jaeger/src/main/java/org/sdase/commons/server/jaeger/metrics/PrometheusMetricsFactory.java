package org.sdase.commons.server.jaeger.metrics;

import io.dropwizard.lifecycle.Managed;
import io.jaegertracing.internal.metrics.Counter;
import io.jaegertracing.internal.metrics.Gauge;
import io.jaegertracing.internal.metrics.Timer;
import io.jaegertracing.spi.MetricsFactory;
import io.prometheus.client.CollectorRegistry;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PrometheusMetricsFactory implements MetricsFactory, Managed {
  private Map<String, io.prometheus.client.Counter> counters = new HashMap<>();
  private Map<String, io.prometheus.client.Histogram> histograms = new HashMap<>();
  private Map<String, io.prometheus.client.Gauge> gauges = new HashMap<>();

  @Override
  public Counter createCounter(String name, Map<String, String> tags) {
    Set<String> labels = tags.keySet();
    String[] labelValues = labels.stream().map(tags::get).toArray(String[]::new);

    io.prometheus.client.Counter counter =
        counters.computeIfAbsent(
            name,
            n ->
                io.prometheus.client.Counter.build(n, n)
                    .labelNames(labels.toArray(new String[0]))
                    .register());

    return delta -> counter.labels(labelValues).inc(delta);
  }

  @Override
  public Timer createTimer(String name, Map<String, String> tags) {
    Set<String> labels = tags.keySet();
    String[] labelValues = labels.stream().map(tags::get).toArray(String[]::new);

    io.prometheus.client.Histogram histogram =
        histograms.computeIfAbsent(
            name,
            n ->
                io.prometheus.client.Histogram.build(n, n)
                    .labelNames(labels.toArray(new String[0]))
                    .register());

    return time -> histogram.labels(labelValues).observe(time);
  }

  @Override
  public Gauge createGauge(String name, Map<String, String> tags) {
    Set<String> labels = tags.keySet();
    String[] labelValues = labels.stream().map(tags::get).toArray(String[]::new);

    io.prometheus.client.Gauge gauge =
        gauges.computeIfAbsent(
            name,
            n ->
                io.prometheus.client.Gauge.build(n, n)
                    .labelNames(labels.toArray(new String[0]))
                    .register());

    return amount -> gauge.labels(labelValues).set(amount);
  }

  @Override
  public void start() {
    // nothing to do
  }

  @Override
  public void stop() {
    counters.forEach(((n, c) -> CollectorRegistry.defaultRegistry.unregister(c)));
    counters.clear();
    histograms.forEach(((n, t) -> CollectorRegistry.defaultRegistry.unregister(t)));
    histograms.clear();
    gauges.forEach(((n, g) -> CollectorRegistry.defaultRegistry.unregister(g)));
    gauges.clear();
  }
}
