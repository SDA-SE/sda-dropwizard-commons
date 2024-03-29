package org.sdase.commons.server.hibernate.test;

import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import org.sdase.commons.server.dropwizard.bundles.ConfigurationSubstitutionBundle;
import org.sdase.commons.server.hibernate.HibernateBundle;
import org.sdase.commons.server.hibernate.test.model.Person;

public class HibernateNoPackageScanApp extends Application<HibernateITestConfiguration> {

  private final HibernateBundle<HibernateITestConfiguration> hibernateBundle =
      HibernateBundle.builder()
          .withConfigurationProvider(HibernateITestConfiguration::getDatabase)
          .withEntityClasses(Person.class)
          .build();

  public static void main(String[] args) throws Exception {
    new HibernateNoPackageScanApp().run(args);
  }

  @Override
  public void initialize(Bootstrap<HibernateITestConfiguration> bootstrap) {
    bootstrap.addBundle(ConfigurationSubstitutionBundle.builder().build());
    bootstrap.addBundle(hibernateBundle);
    bootstrap.addCommand(new DbMigrationCommand());
  }

  @Override
  public void run(HibernateITestConfiguration configuration, Environment environment) {
    environment.jersey().register(new PersonEndPoint(hibernateBundle.sessionFactory()));
  }
}
