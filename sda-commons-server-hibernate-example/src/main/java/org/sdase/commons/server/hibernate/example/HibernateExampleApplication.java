package org.sdase.commons.server.hibernate.example;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.sdase.commons.server.hibernate.HibernateBundle;
import org.sdase.commons.server.hibernate.example.db.DbMigrationCommand;
import org.sdase.commons.server.hibernate.example.db.manager.PersonManager;
import org.sdase.commons.server.hibernate.example.db.model.PersonEntity;
import org.sdase.commons.server.hibernate.example.rest.PersonsEndPoint;

/** Example application to show how to initialize and use the Hibernate Bundle */
public class HibernateExampleApplication extends Application<HibernateExampleConfiguration> {

  /**
   * initialization of the Hibernate bundle. Be sure to import {@link HibernateBundle} (from
   * sda-commons) and not {@link io.dropwizard.hibernate.HibernateBundle} (default dropwizard)
   */
  private final HibernateBundle<HibernateExampleConfiguration> hibernateBundle =
      HibernateBundle.builder()
          // get database configuration from application configuration
          .withConfigurationProvider(HibernateExampleConfiguration::getDatabase)
          // class scanned for hibernate annotations as database entity model
          .withEntityScanPackageClass(PersonEntity.class)
          .build();

  @Override
  public void initialize(Bootstrap<HibernateExampleConfiguration> bootstrap) {
    // Db Migration command should be implemented to allow flyway database
    // migration
    bootstrap.addCommand(new DbMigrationCommand());
    // register bundle so that it will be initialized correctly
    bootstrap.addBundle(hibernateBundle);
  }

  @Override
  public void run(HibernateExampleConfiguration configuration, Environment environment) {
    // the example uses an REST endpoint that again access the hibernate
    // database
    // see https://www.dropwizard.io/1.0.0/docs/manual/hibernate.html, section
    // Transactional Resource Methods
    environment
        .jersey()
        .register(new PersonsEndPoint(new PersonManager(hibernateBundle.sessionFactory())));
  }
}
