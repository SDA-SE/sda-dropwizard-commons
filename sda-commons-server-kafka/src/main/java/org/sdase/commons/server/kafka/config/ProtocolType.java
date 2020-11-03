package org.sdase.commons.server.kafka.config;

public enum ProtocolType {
  PLAINTEXT,
  SSL,
  SASL_PLAINTEXT,
  SASL_SSL;

  /**
   * Tells whether the {@link ProtocolType} is a SASL type or not.
   *
   * @param type the type to check
   * @return if true, the type is a SASL type
   */
  public static boolean isSasl(ProtocolType type) {
    return type == SASL_PLAINTEXT || type == SASL_SSL;
  }
}
