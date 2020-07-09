package org.sdase.commons.server.security.validation;

import io.dropwizard.jetty.ConnectorFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.ServerFactory;
import io.dropwizard.util.DataSize;
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
    private static final DataSize HEADER_CACHE_SIZE = DataSize.bytes(512);
    private static final DataSize OUTPUT_BUFFER_SIZE = DataSize.kibibytes(32);
    private static final DataSize MAX_REQUEST_HEADER_SIZE = DataSize.kibibytes(8);
    private static final DataSize MAX_RESPONSE_HEADER_SIZE = DataSize.kibibytes(8);
    private static final DataSize INPUT_BUFFER_SIZE = DataSize.kibibytes(8);
    private static final DataSize MIN_BUFFER_POOL_SIZE = DataSize.bytes(64);
    private static final DataSize BUFFER_POOL_INCREMENT = DataSize.kibibytes(1);
    private static final DataSize MAX_BUFFER_POOL_SIZE = DataSize.kibibytes(64);
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

  private void check(String paramName, DataSize actual, DataSize maxAllowed) {
    if (actual.toBytes() > maxAllowed.toBytes()) {
      trackViolation(paramName, actual, maxAllowed);
    }
  }

  private void trackViolation(String param, DataSize actual, DataSize maxAllowed) {
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
