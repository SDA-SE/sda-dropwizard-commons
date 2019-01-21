# SDA Commons Server Prometheus Example

This example module shows a dummy implementation of a 
[business service](./src/main/java/org/sdase/commons/server/prometheus/example/MyServiceWithMetrics.java) that records
metrics in three different types:

- A `Histogram` tracks durations
- A `Counter` tracks the number of invocations or events (e.g. successful invocations)
- A `Gauge` tracks the current value of a state, e.g. used memory, free disk space or business data