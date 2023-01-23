package org.sdase.commons.server.spring.data.mongo.compatibility.app;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.sdase.commons.server.spring.data.mongo.MongoConfiguration;
import org.sdase.commons.server.spring.data.mongo.SpringDataMongoBundle;
import org.sdase.commons.server.spring.data.mongo.compatibility.model.MyEntity;
import org.sdase.commons.server.spring.data.mongo.compatibility.model.MyEntityWithGenerics;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.repository.CrudRepository;

public class CompatibilityTestApp extends Application<CompatibilityTestApp.Config> {

  private final SpringDataMongoBundle<Config> springDataMongoBundle =
      SpringDataMongoBundle.builder()
          .withConfigurationProvider(Config::getSpringDataMongo)
          .withMorphiaCompatibility()
          .build();

  private MyEntityRepository myEntityRepository;
  private MyEntityWithGenericsRepository myEntityWithGenericsRepository;

  @Override
  public void initialize(Bootstrap<Config> bootstrap) {
    bootstrap.addBundle(springDataMongoBundle);
  }

  @Override
  public void run(Config configuration, Environment environment) {
    myEntityRepository = springDataMongoBundle.createRepository(MyEntityRepository.class);
    myEntityWithGenericsRepository =
        springDataMongoBundle.createRepository(MyEntityWithGenericsRepository.class);
  }

  public MongoOperations getMongoOperations() {
    return springDataMongoBundle.getMongoOperations();
  }

  public MyEntityRepository getMyEntityRepository() {
    return myEntityRepository;
  }

  public MyEntityWithGenericsRepository getMyEntityWithGenericsRepository() {
    return myEntityWithGenericsRepository;
  }

  public static class Config extends Configuration {

    @Valid @NotNull private MongoConfiguration springDataMongo = new MongoConfiguration();

    public MongoConfiguration getSpringDataMongo() {
      return springDataMongo;
    }

    public Config setSpringDataMongo(MongoConfiguration springDataMongo) {
      this.springDataMongo = springDataMongo;
      return this;
    }
  }

  public interface MyEntityRepository extends CrudRepository<MyEntity, String> {}

  public interface MyEntityWithGenericsRepository
      extends CrudRepository<MyEntityWithGenerics, String> {}
}
