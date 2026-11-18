package org.sdase.commons.server.kafka.secure;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.common.errors.AuthenticationException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/** Positive and negative authentication checks (requires Docker/Testcontainers). */
class SecureKafkaAuthenticationIT {
  @Nested
  class SaslPlain {
    @RegisterExtension
    @Order(0)
    static final SecureKafkaTestResource kafka =
        SecureKafkaTestResource.saslPlain("kafkaclient", "client-secret");

    @Test
    void validLoginAndWrongPasswordRejected() throws Exception {
      assertPositiveAndNegative(kafka);
    }
  }

  @Nested
  class SaslScram {
    @RegisterExtension
    @Order(0)
    static final SecureKafkaTestResource kafka =
        SecureKafkaTestResource.scramSha512("kafkaclient", "client-secret");

    @Test
    void validLoginAndWrongPasswordRejected() throws Exception {
      assertPositiveAndNegative(kafka);
    }
  }

  private static void assertPositiveAndNegative(SecureKafkaTestResource kafka) throws Exception {
    try (Admin admin = Admin.create(kafka.clientProperties())) {
      admin.describeCluster().nodes().get(10, TimeUnit.SECONDS);
    }

    Properties invalid = kafka.clientProperties();
    String jaas = invalid.getProperty("sasl.jaas.config");
    invalid.put("sasl.jaas.config", jaas.replace("client-secret", "wrong-password"));
    invalid.put("request.timeout.ms", "3000");
    invalid.put("default.api.timeout.ms", "5000");

    try (Admin admin = Admin.create(invalid)) {
      ExecutionException ex =
          assertThrows(
              ExecutionException.class,
              () -> admin.describeCluster().nodes().get(10, TimeUnit.SECONDS));
      if (!(ex.getCause() instanceof AuthenticationException)) {
        throw new AssertionError("Expected authentication failure, got " + ex.getCause());
      }
    }
  }
}
