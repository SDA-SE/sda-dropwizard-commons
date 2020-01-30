package org.sdase.commons.server.hibernate;

import io.dropwizard.db.DataSourceFactory;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service to use with a custom Command to initiate database migrations from the command line. An
 * implementation may be:
 *
 * <pre>
 *    <code>public class DbMigrationCommand extends ConfiguredCommand&lt;MyAppConfig&gt; {
 *        &#x40;Override
 *        protected void run(Bootstrap&lt;MyAppConfig&gt; bootstrap, Namespace namespace, MyAppConfig configuration) {
 *           new DbMigrationService(configuration.getDatabase()).migrateDatabase();
 *        }
 *    }
 *    </code>
 * </pre>
 *
 * <p>
 */
public class DbMigrationService {

  public static final String DEFAULT_COMMAND_NAME = "migrateDB";

  public static final String DEFAULT_COMMAND_DOC = "Migrate DB to the actual schema version.";

  private static final Logger LOG = LoggerFactory.getLogger(DbMigrationService.class);

  private DataSourceFactory database;

  public DbMigrationService(DataSourceFactory database) {
    this.database = database;
  }

  public void migrateDatabase() {

    String databaseUrl = database.getUrl();
    String databaseSchema = database.getProperties().getOrDefault("currentSchema", "public");
    LOG.info(
        "Starting database migration for schema {} using database {}", databaseSchema, databaseUrl);
    Configuration configuration =
        new FluentConfiguration()
            .dataSource(databaseUrl, database.getUser(), database.getPassword())
            .schemas(databaseSchema);
    Flyway flyway = new Flyway(configuration);
    flyway.migrate();
    LOG.info("Database migration successful.");
  }
}
