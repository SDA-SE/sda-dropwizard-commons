# SDA Commons Server Prometheus

[![javadoc](https://javadoc.io/badge2/org.sdase.commons/sda-commons-server-prometheus/javadoc.svg)](https://javadoc.io/doc/org.sdase.commons/sda-commons-server-prometheus)

The module `sda-commons-server-prometheus` provides an admin endpoint to serve metrics and health check results in a format that Prometheus can read. 
The endpoint is available at the applications admin port at `/metrics/prometheus`.

## Provided metrics

Default metrics that are provided at `/metrics/prometheus`:

| Metric name                                 | Labels                  | Description                                                                                                                     | Source                                    | Deprecated    |
|---------------------------------------------|-------------------------|---------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------|---------------|
| **`jvm_`***                                 |                         | Multiple metrics about the JVM                                                                                                  | Bridged from Dropwizard                   | Since v5.3.13 |
| **`io_dropwizard_jetty_`**                  |                         | Multiple metrics from the embedded Jetty server                                                                                 | Bridged from Dropwizard                   |               |
| **`io_dropwizard_db_`**                     |                         | Multiple metrics from the database if a database is used                                                                        | Bridged from Dropwizard                   |               |
| **`healthcheck_status`**                    | _`name`_                | Metrics that represent the state of the health checks                                                                           | `HealthCheckMetricsCollector`             |               | 

A filter that extracts the consumer from the HTTP headers should add `Consumer-Name` to the request properties. That
   filter is not part of the `PrometheusBundle`.

## Micrometer Metrics

We are slowly shifting over from Dropwizard-Metrics to [Micrometer](https://micrometer.io/). The
following metrics are available so far.

### JVM

| Metric Name                                  | Labels                  | Description                                                                                                                                                                       | Source                  |
|----------------------------------------------|-------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------|
| `jvm_classes_loaded_classes`                 |                         | The number of classes that are currently loaded in the Java virtual machine.                                                                                                      | Bridged from Micrometer |
| `jvm_classes_unloaded_classes`               |                         | The total number of classes unloaded since the Java virtual machine has started execution.                                                                                        | Bridged from Micrometer |
| `jvm_buffer_count_buffers`                   | `id`                    | An estimate of the number of buffers in the pool.                                                                                                                                 | Bridged from Micrometer |
| `jvm_buffer_memory_used_bytes`               | `id`                    | An estimate of the memory that the Java virtual machine is using for this buffer pool.                                                                                            | Bridged from Micrometer |
| `jvm_buffer_total_capacity_bytes`            | `id`                    | An estimate of the total capacity of the buffers in this pool.                                                                                                                    | Bridged from Micrometer |
| `jvm_memory_used_bytes`                      | `id`, `area`            | The amount of used memory.                                                                                                                                                        | Bridged from Micrometer |
| `jvm_memory_committed_bytes`                 | `id`, `area`            | The amount of memory in bytes that is committed for the Java virtual machine to use.                                                                                              | Bridged from Micrometer |
| `jvm_memory_max_bytes`                       | `id`, `area`            | The maximum amount of memory in bytes that can be used for memory management.                                                                                                     | Bridged from Micrometer |
| `jvm_gc_max_data_size_bytes`                 |                         | Max size of long-lived heap memory pool.                                                                                                                                          | Bridged from Micrometer |
| `jvm_gc_live_data_size_bytes`                |                         | Size of long-lived heap memory pool after reclamation.                                                                                                                            | Bridged from Micrometer |
| `jvm_gc_memory_allocated_bytes_total`        |                         | Incremented for an increase in the size of the (young) heap memory pool after one GC to before the next.                                                                          | Bridged from Micrometer |
| `jvm_gc_memory_promoted_bytes_total`         |                         | Count of positive increases in the size of the old generation memory pool before GC to after GC.                                                                                  | Bridged from Micrometer |
| `jvm_gc_concurrent_phase_time_seconds_count` | `gc`, `action`, `cause` | Time spent in concurrent phase.                                                                                                                                                   | Bridged from Micrometer |
| `jvm_gc_concurrent_phase_time_seconds_sum`   | `gc`, `action`, `cause` |                                                                                                                                                                                   | Bridged from Micrometer |
| `jvm_gc_concurrent_phase_time_seconds_max`   | `gc`, `action`, `cause` |                                                                                                                                                                                   | Bridged from Micrometer |
| `jvm_gc_pause_seconds_count`                 | `gc`, `action`, `cause` | Time spent in GC pause.                                                                                                                                                           | Bridged from Micrometer |
| `jvm_gc_pause_seconds_sum`                   | `gc`, `action`, `cause` |                                                                                                                                                                                   | Bridged from Micrometer |
| `jvm_gc_pause_seconds_max`                   | `gc`, `action`, `cause` |                                                                                                                                                                                   | Bridged from Micrometer |
| `system_cpu_count`                           |                         | The number of processors available to the Java virtual machine.                                                                                                                   | Bridged from Micrometer |
| `system_load_average_1m`                     |                         | The sum of the number of runnable entities queued to available processors and the number of runnable entities running on the available processors averaged over a period of time. | Bridged from Micrometer |
| `system_cpu_usage`                           |                         | The "recent cpu usage" of the system the application is running in.                                                                                                               | Bridged from Micrometer |
| `process_cpu_usage`                          |                         | The "recent cpu usage" for the Java Virtual Machine process.                                                                                                                      | Bridged from Micrometer |
| `jvm_threads_peak_threads`                   |                         | The peak live thread count since the Java virtual machine started or peak was reset.                                                                                              | Bridged from Micrometer |
| `jvm_threads_daemon_threads`                 |                         | The current number of live daemon threads.                                                                                                                                        | Bridged from Micrometer |
| `jvm_threads_live`                           |                         | The current number of live threads including both daemon and non-daemon threads.                                                                                                  | Bridged from Micrometer |
| `jvm_threads_started_threads_total`          |                         | The total number of application threads started in the JVM.                                                                                                                       | Bridged from Micrometer |
| `jvm_threads_states_threads`                 | `state`                 | The current number of threads.                                                                                                                                                    | Bridged from Micrometer |
| `jvm_classes_loaded`                         |                         | The number of classes that are currently loaded in the Java virtual machine.                                                                                                      | Bridged from Micrometer |
| `jvm_classes_unloaded`                       |                         | The total number of classes unloaded since the Java virtual machine has started execution.                                                                                        | Bridged from Micrometer |


### Kafka

Kafka internal producer metrics have been exposed

| Metric name                                                | Labels                 | Description                                                                                                                            | Source |
|------------------------------------------------------------|------------------------|----------------------------------------------------------------------------------------------------------------------------------------|--------|
| `kafka_producer_batch_split_rate`                          | `client.id`            | The average number of batch splits per second                                                                                          |        |
| `kafka_producer_batch_split_total`                         | `client.id`            | The total number of batch splits                                                                                                       |        |
| `kafka_producer_batch_size_max`                            | `client.id`            | The max number of bytes sent per partition per-request.                                                                                |        |
| `kafka_producer_batch_size_avg`                            | `client.id`            | The average number of bytes sent per partition per-request.                                                                            |        |
| `kafka_producer_buffer_exhausted_rate`                     | `client.id`            | The average per-second number of record sends that are dropped due to buffer exhaustion                                                |        |
| `kafka_producer_buffer_total_bytes`                        | `client.id`            | The maximum amount of buffer memory the client can use (whether or not it is currently used).                                          |        |
| `kafka_producer_buffer_available_bytes`                    | `client.id`            | The total amount of buffer memory that is not being used (either unallocated or in the free list).                                     |        |
| `kafka_producer_buffer_exhausted_total`                    | `client.id`            | The total number of record sends that are dropped due to buffer exhaustion                                                             |        |
| `kafka_producer_bufferpool_wait_time_ns_total`             | `client.id`            | The total time in nanoseconds an appender waits for space allocation.                                                                  |        |
| `kafka_producer_bufferpool_wait_ratio`                     | `client.id`            | The fraction of time an appender waits for space allocation.                                                                           |        |
| `kafka_producer_compression_rate_avg`                      | `client.id`            | The average compression rate of record batches, defined as the average ratio of the compressed batch size over the uncompressed size.  |        |
| `kafka_producer_connection_creation_total`                 | `client.id`            | The total number of new connections established                                                                                        |        |
| `kafka_producer_connection_close_rate`                     | `client.id`            | The number of connections closed per second                                                                                            |        |
| `kafka_producer_connection_count`                          | `client.id`            | The current number of active connections.                                                                                              |        |
| `kafka_producer_connection_creation_rate`                  | `client.id`            | The number of new connections established per second                                                                                   |        |
| `kafka_producer_connection_close_total`                    | `client.id`            | The total number of connections closed                                                                                                 |        |
| `kafka_producer_node_incoming_byte_total`                  | `client.id`, `node.id` | The total number of incoming bytes                                                                                                     |        |
| `kafka_producer_node_request_total`                        | `client.id`, `node.id` | The total number of requests sent                                                                                                      |        |
| `kafka_producer_node_request_size_max`                     | `client.id`, `node.id` | The maximum size of any request sent.                                                                                                  |        |
| `kafka_producer_node_outgoing_byte_total`                  | `client.id`, `node.id` | The total number of outgoing bytes                                                                                                     |        |
| `kafka_producer_node_response_total`                       | `client.id`, `node.id` | The total number of responses received                                                                                                 |        |
| `kafka_producer_node_request_size_avg`                     | `client.id`, `node.id` | The average size of requests sent.                                                                                                     |        |
| `kafka_producer_node_response_rate`                        | `client.id`, `node.id` | The number of responses received per second                                                                                            |        |
| `kafka_producer_node_request_latency_avg`                  | `client.id`, `node.id` | The average latency of a producer request                                                                                              |        |
| `kafka_producer_node_incoming_byte_rate`                   | `client.id`, `node.id` | The number of incoming bytes per second                                                                                                |        |
| `kafka_producer_node_outgoing_byte_rate`                   | `client.id`, `node.id` | The number of outgoing bytes per second                                                                                                |        |
| `kafka_producer_node_request_rate`                         | `client.id`, `node.id` | The number of requests sent per second                                                                                                 |        |
| `kafka_producer_node_request_latency_max`                  | `client.id`, `node.id` | The maximum latency of a producer request                                                                                              |        |
| `kafka_producer_record_queue_time_avg`                     | `client.id`            | The average time in ms record batches spent in the send buffer.                                                                        |        |
| `kafka_producer_record_send_total`                         | `client.id`            | The total number of records sent.                                                                                                      |        |
| `kafka_producer_record_size_max`                           | `client.id`            | The maximum record size                                                                                                                |        |
| `kafka_producer_record_error_rate`                         | `client.id`            | The average per-second number of record sends that resulted in errors                                                                  |        |
| `kafka_producer_record_error_total`                        | `client.id`            | The total number of record sends that resulted in errors                                                                               |        |
| `kafka_producer_records_per_request_avg`                   | `client.id`            | The average number of records per request.                                                                                             |        |
| `kafka_producer_record_size_avg`                           | `client.id`            | The average record size                                                                                                                |        |
| `kafka_producer_record_queue_time_max`                     | `client.id`            | The maximum time in ms record batches spent in the send buffer.                                                                        |        |
| `kafka_producer_record_send_rate`                          | `client.id`            | The average number of records sent per second.                                                                                         |        |
| `kafka_producer_record_retry_total`                        | `client.id`            | The total number of retried record sends                                                                                               |        |
| `kafka_producer_record_retry_rate`                         | `client.id`            | The average per-second number of retried record sends                                                                                  |        |
| `kafka_producer_requests_in_flight`                        | `client.id`            | The current number of in-flight requests awaiting a response.                                                                          |        | 
| `kafka_producer_request_size_avg`                          | `client.id`            | The average size of requests sent.                                                                                                     |        |
| `kafka_producer_request_size_max`                          | `client.id`            | The maximum size of any request sent.                                                                                                  |        |
| `kafka_producer_request_total`                             | `client.id`            | The total number of requests sent.                                                                                                     |        |
| `kafka_producer_request_latency_avg`                       | `client.id`            | The average request latency in ms                                                                                                      |        |
| `kafka_producer_request_latency_max`                       | `client.id`            | The maximum request latency in ms                                                                                                      |        |
| `kafka_producer_request_rate`                              | `client.id`            | The number of requests sent per second                                                                                                 |        |
| `kafka_producer_reauthentication_latency_max`              | `client.id`            | The max latency observed due to re-authentication                                                                                      |        |
| `kafka_producer_produce_throttle_time_max`                 | `client.id`            | The maximum time in ms a request was throttled by a broker                                                                             |        |
| `kafka_producer_produce_throttle_time_avg`                 | `client.id`            | The average time in ms a request was throttled by a broker                                                                             |        |
| `kafka_producer_io_wait_time_ns_total`                     | `client.id`            | The total time the I/O thread spent waiting                                                                                            |        |
| `kafka_producer_incoming_byte_rate`                        | `client.id`            | The number of bytes read off all sockets per second                                                                                    |        |
| `kafka_producer_incoming_byte_total`                       | `client.id`            | The total number of bytes read off all sockets                                                                                         |        |
| `kafka_producer_io_time_ns_avg`                            | `client.id`            | The average length of time for I/O per select call in nanoseconds.                                                                     |        |
| `kafka_producer_io_wait_time_ns_avg`                       | `client.id`            | The average length of time the I/O thread spent waiting for a socket ready for reads or writes in nanoseconds.                         |        |
| `kafka_producer_io_time_ns_total`                          | `client.id`            | The total time the I/O thread spent doing I/O                                                                                          |        |
| `kafka_producer_metadata_age`                              | `client.id`            | The age in seconds of the current producer metadata being used.                                                                        |        |
| `kafka_producer_network_io_rate`                           | `client.id`            | The number of network operations (reads or writes) on all connections per second                                                       |        |
| `kafka_producer_network_io_total`                          | `client.id`            | The total number of network operations (reads or writes) on all connections                                                            |        |
| `kafka_producer_flush_time_ns_total`                       | `client.id`            | Total time producer has spent in flush in nanoseconds.                                                                                 |        |
| `kafka_producer_waiting_threads`                           | `client.id`            | The number of user threads blocked waiting for buffer memory to enqueue their records                                                  |        |
| `kafka_producer_reauthentication_latency_avg`              | `client.id`            | The average latency observed due to re-authentication                                                                                  |        |
| `kafka_producer_metadata_wait_time_ns_total`               | `client.id`            | Total time producer has spent waiting on topic metadata in nanoseconds.                                                                |        |
| `kafka_producer_outgoing_byte_rate`                        | `client.id`            | The number of outgoing bytes sent to all servers per second                                                                            |        |
| `kafka_producer_outgoing_byte_total`                       | `client.id`            | The total number of outgoing bytes sent to all servers                                                                                 |        |
| `kafka_producer_response_rate`                             | `client.id`            | The number of responses received per second                                                                                            |        |
| `kafka_producer_response_total`                            | `client.id`            | The total number of responses received                                                                                                 |        |
| `kafka_producer_txn_send_offsets_time_ns_total`            | `client.id`            | Total time producer has spent in sendOffsetsToTransaction in nanoseconds.                                                              |        |
| `kafka_producer_txn_init_time_ns_total`                    | `client.id`            | Total time producer has spent in initTransactions in nanoseconds.                                                                      |        |
| `kafka_producer_txn_abort_time_ns_total`                   | `client.id`            | Total time producer has spent in abortTransaction in nanoseconds.                                                                      |        |
| `kafka_producer_txn_commit_time_ns_total`                  | `client.id`            | Total time producer has spent in commitTransaction in nanoseconds.                                                                     |        |
| `kafka_producer_txn_begin_time_ns_total`                   | `client.id`            | Total time producer has spent in beginTransaction in nanoseconds.                                                                      |        |
| `kafka_producer_failed_authentication_rate`                | `client.id`            | The number of connections with failed authentication per second                                                                        |        |
| `kafka_producer_failed_authentication_total`               | `client.id`            | The total number of connections with failed authentication                                                                             |        |
| `kafka_producer_failed_reauthentication_total`             | `client.id`            | The total number of failed re-authentication of connections                                                                            |        |
| `kafka_producer_failed_reauthentication_rate`              | `client.id`            | The number of failed re-authentication of connections per second                                                                       |        |
| `kafka_app_info_start_time_ms`                             | `client.id`            | Metric indicating start-time-ms                                                                                                        |        |
| `kafka_producer_select_rate`                               | `client.id`            | The number of times the I/O layer checked for new I/O to perform per second                                                            |        |
| `kafka_producer_select_total`                              | `client.id`            | The total number of times the I/O layer checked for new I/O to perform                                                                 |        |
| `kafka_producer_successful_authentication_rate`            | `client.id`            | The number of connections with successful authentication per second                                                                    |        |
| `kafka_producer_successful_authentication_no_reauth_total` | `client.id`            | The total number of connections with successful authentication where the client does not support re-authentication                     |        |
| `kafka_producer_successful_reauthentication_total`         | `client.id`            | The total number of successful re-authentication of connections                                                                        |        |
| `kafka_producer_successful_authentication_total`           | `client.id`            | The total number of connections with successful authentication                                                                         |        |
| `kafka_producer_successful_reauthentication_rate`          | `client.id`            | The number of successful re-authentication of connections per second                                                                   |        |

Kafka internal consumer metrics have been exposed

| Metric name                                                 | Labels      | Description                                                                                                                                    | Source |
|-------------------------------------------------------------|-------------|------------------------------------------------------------------------------------------------------------------------------------------------|--------|
| `kafka_consumer_failed_reauthentication_rate`               | `client.id` | The number of failed re-authentication of connections per second                                                                               |        |
| `kafka_consumer_connection_close_rate`                      | `client.id` | The number of connections closed per second                                                                                                    |        |
| `kafka_consumer_coordinator_partition_assigned_latency_max` | `client.id` | The max time taken for a partition-assigned rebalance listener callback                                                                        |        |
| `kafka_consumer_fetch_manager_records_consumed_rate`        | `client.id` | The average number of records consumed per second                                                                                              |        |
| `kafka_consumer_fetch_manager_fetch_latency_avg`            | `client.id` | The average time taken for a fetch request.                                                                                                    |        |
| `kafka_consumer_io_wait_time_ns_total`                      | `client.id` | The total time the I/O thread spent waiting                                                                                                    |        |
| `kafka_consumer_successful_reauthentication_rate`           | `client.id` | The number of successful re-authentication of connections per second                                                                           |        |
| `kafka_consumer_coordinator_last_rebalance_seconds_ago`     | `client.id` | The number of seconds since the last successful rebalance event                                                                                |        |
| `kafka_consumer_failed_reauthentication_total`              | `client.id` | The total number of failed re-authentication of connections                                                                                    |        |
| `kafka_consumer_coordinator_commit_latency_max`             | `client.id` | The max time taken for a commit request                                                                                                        |        |
| `kafka_consumer_last_poll_seconds_ago`                      | `client.id` | The number of seconds since the last poll() invocation.                                                                                        |        |
| `kafka_consumer_outgoing_byte_total`                        | `client.id` | The total number of outgoing bytes sent to all servers                                                                                         |        |
| `kafka_consumer_request_rate`                               | `client.id` | The number of requests sent per second                                                                                                         |        |
| `kafka_consumer_request_total`                              | `client.id` | The total number of requests sent                                                                                                              |        |
| `kafka_consumer_failed_authentication_rate`                 | `client.id` | The number of connections with failed authentication per second                                                                                |        |
| `kafka_consumer_response_rate`                              | `client.id` | The number of responses received per second                                                                                                    |        |
| `kafka_consumer_fetch_manager_records_lag_max`              | `client.id` | The maximum lag in terms of number of records for any partition in this window_ NOTE: This is based on current offset and not committed offset |        |
| `kafka_consumer_time_between_poll_max`                      | `client.id` | The max delay between invocations of poll() in milliseconds                                                                                    |        |
| `kafka_consumer_coordinator_join_total`                     | `client.id` | The total number of group joins                                                                                                                |        |
| `kafka_consumer_coordinator_join_time_max`                  | `client.id` | The max time taken for a group rejoin                                                                                                          |        |
| `kafka_consumer_coordinator_rebalance_latency_avg`          | `client.id` | The average time taken for a group to complete a successful rebalance, which may be composed of several failed re-trials until it succeeded    |        |
| `kafka_consumer_coordinator_sync_time_avg`                  | `client.id` | The average time taken for a group sync                                                                                                        |        |
| `kafka_consumer_network_io_total`                           | `client.id` | The total number of network operations (reads or writes) on all connections                                                                    |        |
| `kafka_consumer_reauthentication_latency_avg`               | `client.id` | The average latency observed due to re-authentication                                                                                          |        |
| `kafka_consumer_failed_authentication_total`                | `client.id` | The total number of connections with failed authentication                                                                                     |        |
| `kafka_consumer_fetch_manager_bytes_consumed_rate`          | `client.id` | The average number of bytes consumed per second                                                                                                |        |
| `kafka_consumer_coordinator_partition_revoked_latency_max`  | `client.id` | The max time taken for a partition-revoked rebalance listener callback                                                                         |        |
| `kafka_consumer_coordinator_commit_total`                   | `client.id` | The total number of commit calls                                                                                                               |        |
| `kafka_consumer_fetch_manager_fetch_total`                  | `client.id` | The total number of fetch requests                                                                                                             |        |
| `kafka_consumer_io_time_ns_avg`                             | `client.id` | The average length of time for I/O per select call in nanoseconds                                                                              |        |
| `kafka_consumer_connection_count`                           | `client.id` | The current number of active connections                                                                                                       |        |
| `kafka_consumer_coordinator_rebalance_total`                | `client.id` | The total number of successful rebalance events, each event is composed of several failed re-trials until it succeeded                         |        |
| `kafka_consumer_incoming_byte_total`                        | `client.id` | The total number of bytes read off all sockets                                                                                                 |        |
| `kafka_consumer_fetch_manager_records_lead_min`             | `client.id` | The minimum lead in terms of number of records for any partition in this window                                                                |        |
| `kafka_consumer_committed_time_ns_total`                    | `client.id` | The total time the consumer has spent in committed in nanoseconds                                                                              |        |
| `kafka_consumer_coordinator_partition_assigned_latency_avg` | `client.id` | The average time taken for a partition-assigned rebalance listener callback                                                                    |        |
| `kafka_consumer_select_rate`                                | `client.id` | The number of times the I/O layer checked for new I/O to perform per second                                                                    |        |
| `kafka_consumer_coordinator_rebalance_latency_total`        | `client.id` | The total number of milliseconds this consumer has spent in successful rebalances since creation                                               |        |
| `kafka_consumer_coordinator_failed_rebalance_rate_per_hour` | `client.id` | The number of failed rebalance events per hour                                                                                                 |        |
| `kafka_consumer_successful_authentication_rate`             | `client.id` | The number of connections with successful authentication per second                                                                            |        | 
| `kafka_consumer_fetch_manager_records_per_request_avg`      | `client.id` | The average number of records in each request                                                                                                  |        |
| `kafka_consumer_outgoing_byte_rate`                         | `client.id` | The number of outgoing bytes sent to all servers per second                                                                                    |        |
| `kafka_consumer_connection_close_total`                     | `client.id` | The total number of connections closed                                                                                                         |        |
| `kafka_consumer_coordinator_sync_time_max`                  | `client.id` | The max time taken for a group sync                                                                                                            |        |
| `kafka_consumer_network_io_rate`                            | `client.id` | The number of network operations (reads or writes) on all connections per second                                                               |        |
| `kafka_consumer_coordinator_join_rate`                      | `client.id` | The number of group joins per second                                                                                                           |        |
| `kafka_consumer_fetch_manager_bytes_consumed_total`         | `client.id` | The total number of bytes consumed                                                                                                             |        |
| `kafka_consumer_fetch_manager_fetch_size_avg`               | `client.id` | The average number of bytes fetched per request                                                                                                |        |
| `kafka_consumer_response_total`                             | `client.id` | The total number of responses received                                                                                                         |        |
| `kafka_consumer_coordinator_rebalance_latency_max`          | `client.id` | The max time taken for a group to complete a successful rebalance, which may be composed of several failed re-trials until it succeeded        |        |
| `kafka_consumer_fetch_manager_records_consumed_total`       | `client.id` | The total number of records consumed                                                                                                           |        |
| `kafka_consumer_fetch_manager_fetch_rate`                   | `client.id` | The number of fetch requests per second                                                                                                        |        |
| `kafka_consumer_commit_sync_time_ns_total`                  | `client.id` | The total time the consumer has spent in commitSync in nanoseconds                                                                             |        |
| `kafka_consumer_successful_authentication_no_reauth_total`  | `client.id` | The total number of connections with successful authentication where the client does not support re-authentication                             |        |
| `kafka_consumer_io_wait_time_ns_avg`                        | `client.id` | The average length of time the I/O thread spent waiting for a socket ready for reads or writes in nanoseconds                                  |        |
| `kafka_consumer_incoming_byte_rate`                         | `client.id` | The number of bytes read off all sockets per second                                                                                            |        |
| `kafka_consumer_coordinator_heartbeat_response_time_max`    | `client.id` | The max time taken to receive a response to a heartbeat request                                                                                |        |
| `kafka_consumer_coordinator_failed_rebalance_total`         | `client.id` | The total number of failed rebalance events                                                                                                    |        |
| `kafka_consumer_coordinator_rebalance_rate_per_hour`        | `client.id` | The number of successful rebalance events per hour, each event is composed of several failed re-trials until it succeeded                      |        |
| `kafka_consumer_request_size_max`                           | `client.id` | The maximum size of any request sent                                                                                                           |        |
| `kafka_consumer_fetch_manager_fetch_latency_max`            | `client.id` | The max time taken for any fetch request                                                                                                       |        |
| `kafka_consumer_fetch_manager_fetch_throttle_time_avg`      | `client.id` | The average throttle time in ms.                                                                                                               |        |
| `kafka_consumer_coordinator_assigned_partitions`            | `client.id` | The number of partitions currently assigned to this consumer                                                                                   |        |
| `kafka_consumer_coordinator_heartbeat_rate`                 | `client.id` | The number of heartbeats per second                                                                                                            |        |
| `kafka_consumer_coordinator_sync_total                      | `client.id` | The total number of group syncs                                                                                                                |        |
| `kafka_consumer_successful_reauthentication_total`          | `client.id` | The total number of successful re-authentication of connections                                                                                |        |
| `kafka_consumer_fetch_manager_fetch_size_max`               | `client.id` | The maximum number of bytes fetched per request                                                                                                |        |
| `kafka_consumer_coordinator_partition_lost_latency_max`     | `client.id` | The max time taken for a partition-lost rebalance listener callback.                                                                           |        |
| `kafka_consumer_coordinator_last_heartbeat_seconds_ago`     | `client.id` | The number of seconds since the last coordinator heartbeat was sent.                                                                           |        |
| `kafka_consumer_io_time_ns_total`                           | `client.id` | The total time the I/O thread spent doing I/O.                                                                                                 |        |
| `kafka_consumer_coordinator_commit_latency_avg`             | `client.id` | The average time taken for a commit request.                                                                                                   |        |
| `kafka_consumer_coordinator_partition_lost_latency_avg`     | `client.id` | The average time taken for a partition-lost rebalance listener callback                                                                        |        |
| `kafka_consumer_poll_idle_ratio_avg`                        | `client.id` | The average fraction of time the consumer's poll() is idle as opposed to waiting for the user code to process records.                         |        |
| `kafka_consumer_connection_creation_rate`                   | `client.id` | The number of new connections established per second                                                                                           |        |
| `kafka_consumer_connection_creation_total`                  | `client.id` | The total number of new connections established                                                                                                |        |
| `kafka_consumer_select_total`                               | `client.id` | The total number of times the I/O layer checked for new I/O to perform                                                                         |        |
| `kafka_consumer_reauthentication_latency_max`               | `client.id` | The max latency observed due to re-authentication                                                                                              |        |
| `kafka_consumer_request_size_avg`                           | `client.id` | The average size of requests sent                                                                                                              |        |
| `kafka_consumer_fetch_manager_fetch_throttle_time_max`      | `client.id` | The maximum throttle time in ms                                                                                                                |        |
| `kafka_consumer_time_between_poll_avg`                      | `client.id` | The average delay between invocations of poll() in milliseconds                                                                                |        |
| `kafka_consumer_coordinator_join_time_avg`                  | `client.id` | The average time taken for a group rejoin                                                                                                      |        |
| `kafka_consumer_coordinator_sync_rate`                      | `client.id` | The number of group syncs per second                                                                                                           |        |
| `kafka_consumer_coordinator_partition_revoked_latency_avg`  | `client.id` | The average time taken for a partition-revoked rebalance listener callback                                                                     |        |
| `kafka_consumer_successful_authentication_total`            | `client.id` | The total number of connections with successful authentication                                                                                 |        |
| `kafka_consumer_coordinator_heartbeat_total`                | `client.id` | The total number of heartbeats                                                                                                                 |        |


### MongoDB


| Metric Name                         | Labels                                                           | Description                                                                          | Source                   |
|-------------------------------------|------------------------------------------------------------------|--------------------------------------------------------------------------------------|--------------------------|
| `mongodb.driver.pool.waitqueuesize` | `cluster.id`, `server.address`                                   | The current size of the wait queue for a connection from the pool                    | Bridged from Micrometer  |
| `mongodb.driver.pool.checkedout`    | `cluster.id`, `server.address`                                   | The count of connections that are currently in use                                   | Bridged from Micrometer  |
| `mongodb.driver.pool.size`          | `cluster.id`, `server.address`                                   | The current size of the connection pool, including idle and and in-use members       | Bridged from Micrometer  |
| `mongodb.driver.commands`           | `cluster.id`, `server.address` `collection`, `command`, `status` | Timer of mongodb commands                                                            | Bridged from Micrometer  |


### Circuit Breaker

More details about resilience4j circuit breaker metrics can be found [here](https://resilience4j.readme.io/docs/micrometer).

| Metric Name                                             | Labels  | Description                                                                                   | Source                    |
|---------------------------------------------------------|---------|-----------------------------------------------------------------------------------------------|---------------------------|
| `resilience4j_circuitbreaker_buffered_calls`            | `kind`  | The number of buffered failed calls stored in the ring buffer                                 | Bridged from Micrometer   |
| `resilience4j_circuitbreaker_slow_calls`                | `kind`  | The number of slow successful which were slower than a certain threshold                      | Bridged from Micrometer   |
| `resilience4j_circuitbreaker_calls_seconds`             | `kind`  | Total number of successful calls by kind summary                                              | Bridged from Micrometer   |
| `resilience4j_circuitbreaker_calls_seconds_max`         | `kind`  | Total number of successful calls by kind                                                      | Bridged from Micrometer   |
| `resilience4j_circuitbreaker_state`                     | `sate`  | The states of the circuit breaker                                                             | Bridged from Micrometer   |
| `resilience4j_circuitbreaker_slow_call_rate`            |         | The slow call of the circuit breaker                                                          | Bridged from Micrometer   |
| `resilience4j_circuitbreaker_calls_bucket`              | `kind`  | **Deprecated** use `resilience4j_circuitbreaker_calls_seconds` Total number of calls by kind  | Prometheus                |
| `resilience4j_circuitbreaker_failure_rate`              |         | The failure rate of the circuit breaker                                                       | Bridged from Micrometer   |
| `resilience4j_circuitbreaker_not_permitted_calls_total` | `kind`  | Total number of not permitted calls                                                           | Bridged from Micrometer   |
| `resilience4j_circuitbreaker_calls_created`             | `kind`  | **Deprecated** Total number of calls by kind                                                  | Bridged from Micrometer   |


### Jetty

| Metric Name                      | Labels             | Description                                                                 | Source                    |
|----------------------------------|--------------------|-----------------------------------------------------------------------------|---------------------------|
| `jetty_connections_current`      | `connector_name`   | The current number of open Jetty connections                                | Bridged from Micrometer   |
| `jetty_connections_max`          | `connector_name`   | The maximum number of observed connections over a rolling 2-minute interval | Bridged from Micrometer   |
| `jetty_connections_messages_in`  | `connector_name`   | Messages received by tracked connections                                    | Bridged from Micrometer   |
| `jetty_connections_messages_out` | `connector_name`   | Messages sent by tracked connections                                        | Bridged from Micrometer   |
| `jetty_connections_bytes_in`     | `connector_name`   | Bytes received by tracked connections                                       | Bridged from Micrometer   |
| `jetty_connections_bytes_out`    | `connector_name`   | Bytes sent by tracked connections                                           | Bridged from Micrometer   |

### Request Metrics

| Metric Name                                                     | Labels                                                        | Description                                                                                                                               | Source                  | Note                                    |
|-----------------------------------------------------------------|---------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------|-------------------------|-----------------------------------------|
| `http_server_requests_seconds_count`                            | `exception`, `method`, `outcome`, `status`, `uri`             | Total number of requests your application received at this endpoint.                                                                      | Bridged from Micrometer |                                         |
| `http_server_requests_seconds_sum`                              | `exception`, `method`, `outcome`, `status`, `uri`             | Sum of the duration of every request your application received at this endpoint.                                                          | Bridged from Micrometer |                                         |
| `http_server_requests_seconds_max`                              | `exception`, `method`, `outcome`, `status`, `uri`             | Maximum request duration during a time window. The value resets to 0 when a new time window starts. The default time window is 2 minutes. | Bridged from Micrometer |                                         |
| `http_server_requests_seconds_bucket`                           | `exception`, `method`, `outcome`, `status`, `uri`, `le`       | Histogram bucket over every request your application received at this endpoint.                                                           | Bridged from Micrometer | enableRequestHistogram needs to be set. |
| `http_server_requests_seconds`                                  | `exception`, `method`, `outcome`, `status`, `uri`, `quantile` | Quantile over every request your application received at this endpoint.                                                                   | Bridged from Micrometer | requestPercentiles needs to be set.     |
| `apache_http_client_request_duration_seconds`                   | `manager`, `method`, `name`, `quantile`                       | Apache request duration summary.                                                                                                          | Bridged from Micrometer |                                         |
| `jetty_connections`                                             | `factory`, `port`, `quantile`                                 | Jetty Connections summary.                                                                                                                | Bridged from Micrometer |                                         |
| `io_dropwizard_jetty_MutableServletContextHandler_get_requests` | `quantile`                                                    | io_dropwizard_jetty_MutableServletContextHandler_get_requests summary.                                                                    | Bridged from Micrometer |                                         |

## Health Checks

All health checks are provided as a Gauge metric `healthcheck_status` and are included in the metrics endpoint.
The name of the Health Check is used as label `name`.
The metric value is `1.0` for healthy and `0.0` for unhealthy. Example:

```
healthcheck_status{name="hibernate",} 1.0
healthcheck_status{name="disk_space",} 0.0
```

Currently, health checks are evaluated when their status is requested. Slow health checks should be annotated with 
[`com.codahale.metrics.health.annotation.Async`](https://github.com/dropwizard/metrics/blob/v4.0.2/metrics-healthchecks/src/main/java/com/codahale/metrics/health/annotation/Async.java)
to avoid blocking collection of the results.

## Usage

The [`PrometheusBundle`](https://github.com/SDA-SE/sda-dropwizard-commons/tree/main/sda-commons-server-prometheus/src/main/java/org/sdase/commons/server/prometheus/PrometheusBundle.java) has to be added in
the application:

```java
import PrometheusBundle;
import io.dropwizard.core.Application;

public class MyApplication extends Application<MyConfiguration> {
   
    public static void main(final String[] args) {
        new MyApplication().run(args);
    }

   @Override
   public void initialize(Bootstrap<MyConfiguration> bootstrap) {
      // ...
      bootstrap.addBundle(PrometheusBundle.builder().build());
      // ...
   }

   @Override
   public void run(MyConfiguration configuration, Environment environment) {
      // ...
   }
}
```
 
### Server Request Histogram and Percentile

It is optional possible to active histogram bucket or percentile output for the `http_server_requests` metric.
It is only possible to activate one of the two because of limitations in prometheus.

For this it is necessary to set `requestPercentiles` or `enableRequestHistogram` in the configuration.

```yaml
prometheus:
  requestPercentiles: [0.05,0.95]
```
or
```yaml
prometheus:
  enableRequestHistogram: true
```

additionally it is optionally possible to set  `requestDigitsOfPrecision` which describes digits of precision to maintain for percentile approximations.

```yaml
prometheus:
  requestDigitsOfPrecision: 5
```

and it is also possible to set `serviceLevelObjectives` to define specific boundaries for the histogramm.
The values are in nanoseconds.

```yaml
prometheus:
  enableRequestHistogram: true
  requestSlos: [2000000000, 3000000000]
```

Default configuration that is always active is:

```yaml
prometheus:
  requestPercentiles: [0.25, 0.5, 0.75, 0.9, 0.95, 0.99]
  requestDigitsOfPrecision: 3
```

The usage of the [`PrometheusBundle`](https://github.com/SDA-SE/sda-dropwizard-commons/tree/main/sda-commons-server-prometheus/src/main/java/org/sdase/commons/server/prometheus/PrometheusBundle.java) changes slightly through this:

```java
import PrometheusBundle;
import io.dropwizard.core.Application;

public class MyApplication extends Application<MyConfiguration> {

    private PrometheusBundle<PrometheusTestConfiguration> prometheusBundle =
        PrometheusBundle.builder()
            .withConfigurationProvider(MyConfiguration::getPrometheus).buildBundle();
  
    public static void main(final String[] args) {
        new MyApplication().run(args);
    }

   @Override
   public void initialize(Bootstrap<MyConfiguration> bootstrap) {
      // ...
      bootstrap.addBundle(prometheusBundle);
      // ...
   }

   @Override
   public void run(MyConfiguration configuration, Environment environment) {
      // ...
   }
}
```
