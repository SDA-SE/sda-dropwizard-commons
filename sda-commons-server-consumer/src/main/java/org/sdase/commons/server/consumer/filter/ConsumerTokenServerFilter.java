package org.sdase.commons.server.consumer.filter;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import java.util.List;

/**
 * A request filter that detects, verifies and provides the consumer token in incoming requests.
 *
 * @deprecated This class will be replaced by {@link
 *     org.sdase.commons.server.dropwizard.filter.ConsumerTokenServerFilter} when removing the
 *     module {@code sda-commons-server-consumer}. To prepare for the upcoming breaking change,
 *     update all references to {@link
 *     org.sdase.commons.server.dropwizard.filter.ConsumerTokenServerFilter} and remove direct
 *     dependencies to {@code sda-commons-server-consumer}.
 */
@Priority(Priorities.AUTHENTICATION - 10) // Before Access-Token authentication
@Deprecated(forRemoval = true)
@SuppressWarnings("java:S2176") // intentionally the same name until removed
public class ConsumerTokenServerFilter
    extends org.sdase.commons.server.dropwizard.filter.ConsumerTokenServerFilter {

  /**
   * @param requireIdentifiedConsumer if an identified customer is required to fulfill requests
   * @param excludeRegex a list of regex pattern for paths that are excluded from the filter
   */
  public ConsumerTokenServerFilter(boolean requireIdentifiedConsumer, List<String> excludeRegex) {
    super(requireIdentifiedConsumer, excludeRegex);
  }
}
