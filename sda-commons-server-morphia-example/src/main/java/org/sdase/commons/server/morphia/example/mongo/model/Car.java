package org.sdase.commons.server.morphia.example.mongo.model;

import org.bson.types.ObjectId;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.IndexOptions;
import dev.morphia.annotations.Indexed;

@Entity("cars")
public class Car {

   @Id
   private ObjectId id;

   private String model;
   private String color;

   @Indexed(options = @IndexOptions(sparse = true)) // create an sparse index on 'sign' field
   private String sign;

   public String getModel() {
      return model;
   }

   public Car setModel(String model) {
      this.model = model;
      return this;
   }

   public String getSign() {
      return sign;
   }

   public Car setSign(String sign) {
      this.sign = sign;
      return this;
   }

   public String getColor() {
      return color;
   }

   public Car setColor(String color) {
      this.color = color;
      return this;
   }

   public ObjectId getId() {
      return id;
   }


}
