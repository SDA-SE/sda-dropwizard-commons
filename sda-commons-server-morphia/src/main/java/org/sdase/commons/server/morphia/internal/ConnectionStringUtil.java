package org.sdase.commons.server.morphia.internal;

import org.apache.commons.lang3.StringUtils;
import org.sdase.commons.server.morphia.MongoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ConnectionStringUtil {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionStringUtil.class);

  private ConnectionStringUtil() {
    // this is a utility class
  }

  static String createConnectionString(MongoConfiguration config) {
    if (StringUtils.isNotBlank(config.getConnectionString())) {
      LOGGER.info("Creating connection using connectionString");
      if (StringUtils.isNotBlank(config.getHosts())
          || StringUtils.isNotBlank(config.getDatabase())
          || StringUtils.isNotBlank(config.getUsername())
          || StringUtils.isNotBlank(config.getPassword())) {
        LOGGER.warn(
            "Please only use the 'connectionString' and no other properties to configure "
                + "your database connection. All other properties are ignored.");
      }
      return config.getConnectionString();
    }

    LOGGER.info("Creating connection using mongodb:// URL from config");
    StringBuilder connectionStringBuilder = new StringBuilder();

    connectionStringBuilder
        .append("mongodb://")
        .append(buildCredentialsUriPartIfNeeded(config))
        .append(config.getHosts())
        .append("/")
        .append(config.getDatabase());

    if (StringUtils.isNotBlank(config.getOptions())) {
      connectionStringBuilder.append("?").append(config.getOptions());
    }

    return connectionStringBuilder.toString();
  }

  private static StringBuilder buildCredentialsUriPartIfNeeded(MongoConfiguration configuration) {
    if (StringUtils.isNotBlank(configuration.getUsername())
        && StringUtils.isNotBlank(configuration.getPassword())) {
      return new StringBuilder()
          .append(configuration.getUsername())
          .append(":")
          .append(configuration.getPassword())
          .append("@");
    }
    return new StringBuilder();
  }
}
