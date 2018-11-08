package org.sdase.commons.server.hibernate.test;

import org.sdase.commons.server.hibernate.DbMigrationService;
import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;

public class DbMigrationCommand extends ConfiguredCommand<HibernateITestConfiguration> {

   public DbMigrationCommand() {
      super(DbMigrationService.DEFAULT_COMMAND_NAME, DbMigrationService.DEFAULT_COMMAND_DOC);
   }

   @Override
   protected void run(Bootstrap<HibernateITestConfiguration> bootstrap, Namespace namespace,
                      HibernateITestConfiguration configuration) {
      new DbMigrationService(configuration.getDatabase()).migrateDatabase();
   }
}
