package org.sdase.commons.server.s3.testing;

import io.findify.s3mock.S3Mock;
import java.io.IOException;
import java.net.ServerSocket;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * JUnit Test rule for running a AWS S3-compatible object storage alongside the (integration)
 * tests. Use {@link #getEndpoint()} to retrieve the endpoint URL to connect to.
 * </p>
 * <p>
 * Example usage:
 * </p>
 *
 * <pre>
 *     <code>
 *         &#64;ClassRule
 *         public static final S3MockRule S3_MOCK = S3MockRule.builder().build();
 *     </code>
 * </pre>
 */
public class S3MockRule extends ExternalResource {
   private static final Logger LOGGER = LoggerFactory.getLogger(S3MockRule.class);

   private S3Mock s3Mock;
   private Integer port;

   public static Builder builder() {
      return new Builder();
   }

   private S3MockRule() {
      // prevent instantiation without builder
   }

   @Override
   protected void before() {
      port = getFreePort();
      s3Mock = new S3Mock.Builder().withInMemoryBackend().withPort(port).build();
      s3Mock.start();
      LOGGER.info("Started S3 Mock at {}", getEndpoint());
   }

   @Override
   protected void after() {
      s3Mock.stop();
      LOGGER.info("Stopped S3 Mock");
   }

   public String getEndpoint() {
      return "http://localhost:" + getPort();
   }

   public int getPort() {
      return port;
   }

   private int getFreePort() {
      try (ServerSocket socket = new ServerSocket(0)) {
         socket.setReuseAddress(true);
         return socket.getLocalPort();
      } catch (IOException e) {
         // Use a fixed port number as a fallback if an error occurs
         return 28301;
      }
   }

   public static final class Builder {
      private Builder() {
         // prevent instantiation
      }

      public S3MockRule build() {
         return new S3MockRule();
      }
   }
}
