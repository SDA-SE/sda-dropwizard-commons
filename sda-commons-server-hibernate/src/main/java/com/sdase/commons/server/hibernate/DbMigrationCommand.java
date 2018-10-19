package com.sdase.commons.server.hibernate;

import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbMigrationCommand extends ConfiguredCommand<DbMigrationConfiguration> {

   private static final Logger LOG = LoggerFactory.getLogger(DbMigrationCommand.class);

   private static final String COMMAND_NAME = "migrateDB";
   private static final String COMMAND_DESCRIPTION = "Migrate DB to the actual schema version.";


   DbMigrationCommand() {
      super(COMMAND_NAME, COMMAND_DESCRIPTION);
   }

   @Override
   protected void run(Bootstrap<DbMigrationConfiguration> bootstrap, Namespace namespace, DbMigrationConfiguration configuration) {
      DataSourceFactory database = configuration.getDatabase();
      String databaseUrl = database.getUrl();
      String databaseSchema = database.getProperties().getOrDefault("currentSchema", "public");
      LOG.info("Starting database migration for schema {} using database {}", databaseSchema, databaseUrl);
      Flyway flyway = new Flyway();
      flyway.setDataSource(databaseUrl, database.getUser(), database.getPassword());
      flyway.setSchemas(databaseSchema);
      flyway.migrate();
      LOG.info("Database migration successful.");
   }
}
