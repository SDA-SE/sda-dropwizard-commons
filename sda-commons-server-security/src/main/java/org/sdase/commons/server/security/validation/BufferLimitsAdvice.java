package org.sdase.commons.server.security.validation;

import io.dropwizard.jetty.ConnectorFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.ServerFactory;
import io.dropwizard.util.Size;
import java.util.List;
import java.util.Objects;
import org.sdase.commons.server.security.exception.InsecureConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks that secure limits of used {@link io.dropwizard.jetty.HttpConnectorFactory} instances are
 * used to avoid the risk of buffer overflow attacks. This class checks for the risks identified in
 * the security guide as:
 *
 * <ul>
 *   <li>"Risiko: Buffer Overflow"
 * </ul>
 */
public class BufferLimitsAdvice {

  private static final Logger LOG = LoggerFactory.getLogger(BufferLimitsAdvice.class);

  /**
   * The max values we allow for the corresponding configuration in {@link HttpConnectorFactory}.
   */
  private static class Max {
    private static final Size HEADER_CACHE_SIZE = Size.bytes(512);
    private static final Size OUTPUT_BUFFER_SIZE = Size.kilobytes(32);
    private static final Size MAX_REQUEST_HEADER_SIZE = Size.kilobytes(8);
    private static final Size MAX_RESPONSE_HEADER_SIZE = Size.kilobytes(8);
    private static final Size INPUT_BUFFER_SIZE = Size.kilobytes(8);
    private static final Size MIN_BUFFER_POOL_SIZE = Size.bytes(64);
    private static final Size BUFFER_POOL_INCREMENT = Size.kilobytes(1);
    private static final Size MAX_BUFFER_POOL_SIZE = Size.kilobytes(64);
  }

  private List<ConnectorFactory> connectorFactories;

  private boolean onlyLogOnViolation;

  public BufferLimitsAdvice(ServerFactory serverFactory, boolean onlyLogOnViolation) {
    this.onlyLogOnViolation = onlyLogOnViolation;
    this.connectorFactories = ServerFactoryUtil.extractConnectorFactories(serverFactory);
  }

  public void applySecureConfiguration() {
    connectorFactories.stream().filter(Objects::nonNull).forEach(this::applySecureConfiguration);
  }

  private void applySecureConfiguration(ConnectorFactory connectorFactory) {
    if (connectorFactory instanceof HttpConnectorFactory) {
      HttpConnectorFactory httpConnectorFactory = (HttpConnectorFactory) connectorFactory;
      check("headerCacheSize", httpConnectorFactory.getHeaderCacheSize(), Max.HEADER_CACHE_SIZE);
      check("outputBufferSize", httpConnectorFactory.getOutputBufferSize(), Max.OUTPUT_BUFFER_SIZE);
      check(
          "maxRequestHeaderSize",
          httpConnectorFactory.getMaxRequestHeaderSize(),
          Max.MAX_REQUEST_HEADER_SIZE);
      check(
          "maxResponseHeaderSize",
          httpConnectorFactory.getMaxResponseHeaderSize(),
          Max.MAX_RESPONSE_HEADER_SIZE);
      check("inputBufferSize", httpConnectorFactory.getInputBufferSize(), Max.INPUT_BUFFER_SIZE);
      check(
          "minBufferPoolSize",
          httpConnectorFactory.getMinBufferPoolSize(),
          Max.MIN_BUFFER_POOL_SIZE);
      check(
          "bufferPoolIncrement",
          httpConnectorFactory.getBufferPoolIncrement(),
          Max.BUFFER_POOL_INCREMENT);
      check(
          "maxBufferPoolSize",
          httpConnectorFactory.getMaxBufferPoolSize(),
          Max.MAX_BUFFER_POOL_SIZE);
    } else {
      LOG.warn(
          "Unable to apply secure configuration to connector factory of type {}",
          connectorFactory.getClass());
    }
  }

  private void check(String paramName, Size actual, Size maxAllowed) {
    if (actual.toBytes() > maxAllowed.toBytes()) {
      trackViolation(paramName, actual, maxAllowed);
    }
  }

  private void trackViolation(String param, Size actual, Size maxAllowed) {
    if (onlyLogOnViolation) {
      LOG.warn(
          "{} in HTTP connector is configured to {} exceeding the max allowed value of {}",
          param,
          actual,
          maxAllowed);
    } else {
      throw new InsecureConfigurationException(
          String.format(
              "%s in HTTP connector is configured to %s exceeding the max allowed value of %s",
              param, actual, maxAllowed));
    }
  }
}
