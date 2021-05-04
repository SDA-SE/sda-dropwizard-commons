package org.sdase.commons.server.morphia.example;

import dev.morphia.Datastore;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.sdase.commons.server.morphia.MorphiaBundle;
import org.sdase.commons.server.morphia.example.mongo.CarManager;
import org.sdase.commons.server.morphia.example.mongo.model.Car;

/** Example to use MorphiaBundle as a plain Dropwizard application. */
public class MorphiaScanPackageClassApplication
    extends Application<MorphiaApplicationConfiguration> {

  private CarManager carManager;

  private final MorphiaBundle<MorphiaApplicationConfiguration> morphiaBundle =
      MorphiaBundle.builder()
          .withConfigurationProvider(
              MorphiaApplicationConfiguration
                  ::getMongo) // configuration provider of mongo connection details
          .withEntityScanPackageClass(
              Car.class) // Entity package that is registered in morphia. Morphia will configure the
          // database as defined within the entity e.g. with indexes.
          .build();
  private Datastore datastore;

  public static void main(String[] args) throws Exception {
    new MorphiaScanPackageClassApplication().run(args); // Main to start this application
  }

  @Override
  public void initialize(Bootstrap<MorphiaApplicationConfiguration> bootstrap) {
    bootstrap.addBundle(morphiaBundle); // Add bundle to Dropwizard
  }

  @Override
  public void run(MorphiaApplicationConfiguration configuration, Environment environment) {
    datastore = morphiaBundle.datastore();
    carManager = new CarManager(datastore);
  }

  public CarManager getCarManager() {
    return carManager;
  }

  public Datastore getDatastore() {
    return datastore;
  }
}
