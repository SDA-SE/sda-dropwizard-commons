/*
 * Copyright (c) 2017-2020, Salesforce.com, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 *   disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of Salesforce.com nor the names of its contributors may be used to endorse or promote products
 *   derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Based on https://github.com/salesforce/kafka-junit/blob/9675c18b5221a156821d534129b9f03e6b6779b3/kafka-junit-core/src/main/java/com/salesforce/kafka/test/listeners/SaslPlainListener.java
 */
package com.salesforce.kafka.test.listeners;

import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Define and register a SASL_SCRAM listener on a Kafka broker.
 *
 * <p>NOTE: Kafka reads in the JAAS file as defined by an Environment variable at JVM start up. This
 * property can not be set at run time.
 *
 * <p>In order to make use of this Listener, you **must** start the JVM with the following:
 * -Djava.security.auth.login.config=/path/to/your/jaas.conf
 */
public class SaslScramListener extends AbstractListener<SaslScramListener> {
  private static final Logger logger = LoggerFactory.getLogger(SaslPlainListener.class);

  private String username = "";
  private String password = "";

  /**
   * Constructor. Only purpose is to emit an ERROR log message if the System environment variable
   * java.security.auth.login.config has not be set.
   */
  public SaslScramListener() {
    if (!JaasValidationTool.isJaasEnvironmentValueSet()) {
      logger.error(
          "Missing required environment variable set: " + JaasValidationTool.JAAS_VARIABLE_NAME);
    }
  }

  /**
   * Setter.
   *
   * @param username SASL username to authenticate with.
   * @return SaslScramListener for method chaining.
   */
  public SaslScramListener withUsername(final String username) {
    this.username = username;
    return this;
  }

  /**
   * Setter.
   *
   * @param password SASL password to authenticate with.
   * @return SaslScramListener for method chaining.
   */
  public SaslScramListener withPassword(final String password) {
    this.password = password;
    return this;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  @Override
  public String getProtocol() {
    return "SASL_PLAINTEXT";
  }

  @Override
  public Properties getBrokerProperties() {
    final Properties properties = new Properties();
    properties.put("sasl.enabled.mechanisms", "SCRAM-SHA-256,SCRAM-SHA-512");
    properties.put("sasl.mechanism.inter.broker.protocol", "SCRAM-SHA-512");

    properties.put("security.inter.broker.protocol", "SASL_PLAINTEXT");

    return properties;
  }

  @Override
  public Properties getClientProperties() {
    final Properties properties = new Properties();
    properties.put("sasl.mechanism", "SCRAM-SHA-512");
    properties.put("security.protocol", "SASL_PLAINTEXT");
    properties.put(
        "sasl.jaas.config",
        "org.apache.kafka.common.security.scram.ScramLoginModule required\n"
            + "username=\""
            + username
            + "\"\n"
            + "password=\""
            + password
            + "\";");

    return properties;
  }
}
