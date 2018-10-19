package com.sdase.commons.server.hibernate;

import io.dropwizard.db.DataSourceFactory;

/**
 * <p>
 *    This interface enforces the database configuration in the config.yaml file to be namespaced with "database".
 * </p>
 * <p>
 *    For testing this can be an in memory H2 DB:
 * </p>
 * <pre>{@code
 * // test-config.yaml
 * database:
 *   driverClass: org.h2.Driver
 *   user: sa
 *   password: sa
 *   url: jdbc:h2:mem:test;DB_CLOSE_DELAY=-1
 *   properties:
 *     charSet: UTF-8 // may be omitted, UTF-8 is set as default
 *     hibernate.dialect: org.hibernate.dialect.PostgreSQLDialect // may be omitted, org.hibernate.dialect.PostgreSQLDialect is set as default
 * }</pre>
 * <p>
 *    In production it should be a persistent Postgres that can be filled with environment properties:
 * </p>
 * <pre{@code
 * // config.yaml
 * database:
 *   driverClass: org.postgresql.Driver
 *   user: ${POSTGRES_USER}
 *   password: ${POSTGRES_PASSWORD}
 *   url: ${POSTGRES_URL}
 *   properties:
 *     charSet: UTF-8 // may be omitted, UTF-8 is set as default
 *     hibernate.dialect: org.hibernate.dialect.PostgreSQLDialect // may be omitted, org.hibernate.dialect.PostgreSQLDialect is set as default
 *     currentSchema: ${POSTGRES_SCHEMA}
 * }></pre>
 */
public interface DatabaseConfigurable {

   /**
    * @return the configuration of the database build from config.yaml
    */
   DataSourceFactory getDatabase();
}
