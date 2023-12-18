package org.sdase.commons.server.spring.data.mongo.example;

import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import org.sdase.commons.server.spring.data.mongo.SpringDataMongoBundle;
import org.sdase.commons.server.spring.data.mongo.example.repository.PersonRepository;
import org.springframework.data.mongodb.core.MongoOperations;

public class MyApp extends Application<MyConfiguration> {

  private final SpringDataMongoBundle<MyConfiguration> springDataMongoBundle =
      SpringDataMongoBundle.builder()
          .withConfigurationProvider(MyConfiguration::getSpringDataMongo)
          .build();

  private PersonRepository personRepository;

  @Override
  public void initialize(Bootstrap<MyConfiguration> bootstrap) {
    bootstrap.addBundle(springDataMongoBundle);
  }

  @Override
  public void run(MyConfiguration configuration, Environment environment) {
    personRepository = springDataMongoBundle.createRepository(PersonRepository.class);
  }

  public MongoOperations getMongoOperations() {
    return springDataMongoBundle.getMongoOperations();
  }

  public PersonRepository getPersonRepository() {
    return personRepository;
  }
}
