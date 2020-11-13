package org.sdase.commons.client.jersey.filter;

import java.util.Optional;
import java.util.function.Supplier;
import org.sdase.commons.shared.tracing.ConsumerTracing;

/**
 * A client filter that adds the {@value ConsumerTracing#TOKEN_HEADER} header to the client request.
 */
public class ConsumerTokenHeaderFilter implements AddRequestHeaderFilter {

  private final Supplier<Optional<String>> consumerTokenSupplier;

  public ConsumerTokenHeaderFilter(String consumerToken) {
    this.consumerTokenSupplier = () -> Optional.ofNullable(consumerToken);
  }

  public ConsumerTokenHeaderFilter(Supplier<Optional<String>> consumerTokenSupplier) {
    this.consumerTokenSupplier = consumerTokenSupplier;
  }

  @Override
  public String getHeaderName() {
    return ConsumerTracing.TOKEN_HEADER;
  }

  @Override
  public Optional<String> getHeaderValue() {
    return consumerTokenSupplier.get();
  }
}
