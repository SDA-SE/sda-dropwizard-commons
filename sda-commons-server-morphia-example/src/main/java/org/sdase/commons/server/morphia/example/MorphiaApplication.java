package org.sdase.commons.server.morphia.example;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.sdase.commons.server.morphia.MorphiaBundle;
import org.sdase.commons.server.morphia.example.mongo.CarManager;
import org.sdase.commons.server.morphia.example.mongo.model.Car;
import org.sdase.commons.server.weld.DropwizardWeldHelper;
import xyz.morphia.Datastore;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

@ApplicationScoped // make this application a WELD application
public class MorphiaApplication extends Application<MorphiaApplicationConfiguration> {

   @Inject
   private CarManager carManager; // car manager as example for injection of morphia data store

   private MorphiaBundle<MorphiaApplicationConfiguration> morphiaBundle = MorphiaBundle.builder()
         .withConfigurationProvider(MorphiaApplicationConfiguration::getMongo) // configuration provider of mongo connection details
         .withEntity(Car.class) // Entity that is registered in morphia. Morphia will configure the database as defined within the entity e.g. with indexes.
         .build();

   public static void main(String[] args) throws Exception {
      DropwizardWeldHelper.run(MorphiaApplication.class, args); // Main to start this application
   }

   @Override
   public void initialize(Bootstrap<MorphiaApplicationConfiguration> bootstrap) {
      bootstrap.addBundle(morphiaBundle); // Add bundle to Dropwizard
   }

   @Override
   public void run(MorphiaApplicationConfiguration configuration, Environment environment) {
      // noting to do here
   }

   /**
    * If weld is used, the datastore can be provided within a producer to be
    * injectable. In this example, the datastore is used within the @{@link CarManager}
    * 
    * @return morphia datastore
    */
   @Produces
   Datastore morphiaDatastore() {
      return morphiaBundle.datastore();
   }

   /**
    * Dummy method for demonstration issues only. Normally, the manager is not accessed directly but via
    * a REST endpoint
    * @return CarManager
    */
   CarManager carManager() {
      return carManager;
   }

}
