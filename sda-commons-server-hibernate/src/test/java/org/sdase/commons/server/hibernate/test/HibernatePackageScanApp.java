package org.sdase.commons.server.hibernate.test;

import org.sdase.commons.server.dropwizard.bundles.ConfigurationSubstitutionBundle;
import org.sdase.commons.server.hibernate.HibernateBundle;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class HibernatePackageScanApp extends Application<HibernateITestConfiguration> {

   private final HibernateBundle<HibernateITestConfiguration> hibernateBundle = HibernateBundle.builder()
         .withConfigurationProvider(HibernateITestConfiguration::getDatabase)
         .withEntityScanPackage("org.sdase.commons.server.hibernate.test.model")
         .build();

   public static void main(String[] args) throws Exception {
      new HibernatePackageScanApp().run(args);
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
