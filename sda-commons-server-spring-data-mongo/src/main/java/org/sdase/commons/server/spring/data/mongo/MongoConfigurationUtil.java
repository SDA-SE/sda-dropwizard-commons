package org.sdase.commons.server.spring.data.mongo;

import org.apache.commons.lang3.StringUtils;

class MongoConfigurationUtil {

  private MongoConfigurationUtil() {
    // utility class
  }

  static String buildConnectionString(MongoConfiguration configuration) {
    if (StringUtils.isNotBlank(configuration.getConnectionString())) {
      return configuration.getConnectionString();
    } else {
      return buildConnectionStringFromDetails(configuration);
    }
  }

  private static String buildConnectionStringFromDetails(MongoConfiguration configuration) {
    var connectionString = new StringBuilder("mongodb://");

    if (StringUtils.isNotBlank(configuration.getUsername())
        && StringUtils.isNotBlank(configuration.getPassword())) {
      connectionString
          .append(configuration.getUsername())
          .append(":")
          .append(configuration.getPassword())
          .append("@");
    }
    connectionString
        .append(configuration.getHosts())
        .append("/")
        .append(configuration.getDatabase());
    if (StringUtils.isNotBlank(configuration.getOptions())) {
      connectionString.append("?").append(configuration.getOptions());
    }

    return connectionString.toString();
  }
}
