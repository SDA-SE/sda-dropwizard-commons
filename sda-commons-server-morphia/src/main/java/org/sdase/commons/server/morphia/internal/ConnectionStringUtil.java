package org.sdase.commons.server.morphia.internal;

import org.apache.commons.lang3.StringUtils;
import org.sdase.commons.server.morphia.MongoConfiguration;

class ConnectionStringUtil {

  private ConnectionStringUtil() {
    // this is a utility class
  }

  static String createConnectionString(MongoConfiguration configuration) {

    StringBuilder connectionStringBuilder = new StringBuilder();

    if (StringUtils.isNotBlank(configuration.getConnectionString())) {
      connectionStringBuilder.append(configuration.getConnectionString());
    } else {
      connectionStringBuilder
          .append("mongodb://")
          .append(buildCredentialsUriPartIfNeeded(configuration))
          .append(configuration.getHosts())
          .append("/")
          .append(configuration.getDatabase());

      if (StringUtils.isNotBlank(configuration.getOptions())) {
        connectionStringBuilder.append("?").append(configuration.getOptions());
      }
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
