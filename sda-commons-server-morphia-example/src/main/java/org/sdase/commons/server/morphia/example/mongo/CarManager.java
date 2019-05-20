package org.sdase.commons.server.morphia.example.mongo;

import org.sdase.commons.server.morphia.example.mongo.model.Car;
import dev.morphia.Datastore;
import dev.morphia.Key;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;

/**
 * Example manager that processes operations on a mongo/morphia database.
 * The manager uses WELD DI to inject the morphia data store
 */
@ApplicationScoped
public class CarManager {

   private final Datastore datastore;

   @Inject
   public CarManager(Datastore datastore) {
      this.datastore = datastore;
   }

   /**
    * Stores a car object in the mongo database
    * @param car car to store
    * @return the key of the stored car
    */
   public Key<Car> store(Car car) {
      return datastore.save(car);
   }

   /**
    * get all cars that are registered in Hamburg
    * @return list with cars.
    */
   public List<Car> hamburgCars() {
      return datastore.createQuery(Car.class).field("sign").startsWith("HH").find().toList();
   }

}
