package org.sdase.commons.server.hibernate.example.db;

import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import org.sdase.commons.server.hibernate.DbMigrationService;
import org.sdase.commons.server.hibernate.example.HibernateExampleConfiguration;

/** Default command for database migrations as described in hibernate bundle documentation */
public class DbMigrationCommand extends ConfiguredCommand<HibernateExampleConfiguration> {

  public DbMigrationCommand() {
    super(DbMigrationService.DEFAULT_COMMAND_NAME, DbMigrationService.DEFAULT_COMMAND_DOC);
  }

  @Override
  protected void run(
      Bootstrap<HibernateExampleConfiguration> bootstrap,
      Namespace namespace,
      HibernateExampleConfiguration configuration) {
    new DbMigrationService(configuration.getDatabase()).migrateDatabase();
  }
}
