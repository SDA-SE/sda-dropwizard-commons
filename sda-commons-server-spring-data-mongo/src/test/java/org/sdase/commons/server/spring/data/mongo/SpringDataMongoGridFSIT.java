package org.sdase.commons.server.spring.data.mongo;

import static de.flapdoodle.embed.mongo.distribution.Version.Main.V4_4;
import static de.flapdoodle.embed.mongo.distribution.Version.Main.V5_0;
import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ConfigOverride.randomPorts;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.mongodb.core.query.Query.query;
import static org.springframework.data.mongodb.gridfs.GridFsCriteria.whereFilename;
import static org.springframework.data.mongodb.gridfs.GridFsCriteria.whereMetaData;

import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.mongo.testing.MongoDbClassExtension;
import org.sdase.commons.server.spring.data.mongo.example.MyApp;
import org.sdase.commons.server.spring.data.mongo.example.MyConfiguration;
import org.sdase.commons.server.spring.data.mongo.example.model.Person;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.gridfs.GridFsOperations;

abstract class SpringDataMongoGridFSIT {

  static class MongoDb44Test extends SpringDataMongoGridFSIT {
    @RegisterExtension
    @Order(0)
    static final MongoDbClassExtension mongo =
        MongoDbClassExtension.builder().withVersion(V4_4).build();

    @RegisterExtension
    @Order(1)
    static final DropwizardAppExtension<MyConfiguration> DW =
        new DropwizardAppExtension<>(
            MyApp.class,
            null,
            randomPorts(),
            config("springDataMongo.connectionString", mongo::getConnectionString));

    @Override
    DropwizardAppExtension<MyConfiguration> getDW() {
      return DW;
    }
  }

  static class MongoDb50Test extends SpringDataMongoGridFSIT {
    @RegisterExtension
    @Order(0)
    static final MongoDbClassExtension mongo =
        MongoDbClassExtension.builder().withVersion(V5_0).build();

    @RegisterExtension
    @Order(1)
    static final DropwizardAppExtension<MyConfiguration> DW =
        new DropwizardAppExtension<>(
            MyApp.class,
            null,
            randomPorts(),
            config("springDataMongo.connectionString", mongo::getConnectionString));

    @Override
    DropwizardAppExtension<MyConfiguration> getDW() {
      return DW;
    }
  }

  @Test
  void shouldStoreSimpleFile() throws IOException {
    GridFsOperations gridFsOperations = getGridFsOperations();

    try (InputStream is =
        new BufferedInputStream(new ClassPathResource("./example-file.txt").getInputStream())) {
      gridFsOperations.store(is, "example-file.txt");
    }

    var file = gridFsOperations.findOne(query(whereFilename().is("example-file.txt")));
    assertThat(file.getFilename()).isEqualTo("example-file.txt");
  }

  @Test
  void shouldStoreFileWithMetadata() throws IOException {
    GridFsOperations gridFsOperations = getGridFsOperations();

    try (InputStream is =
        new BufferedInputStream(new ClassPathResource("./example-file.txt").getInputStream())) {
      var metaData = new Person().setName("John Doe");

      gridFsOperations.store(is, "example-file.txt", metaData);
    }

    var file = gridFsOperations.findOne(query(whereMetaData("name").is("John Doe")));
    assertThat(file.getFilename()).isEqualTo("example-file.txt");
  }

  private GridFsOperations getGridFsOperations() {
    MyApp app = getDW().getApplication();
    GridFsOperations gridFsOperations = app.getGridFsOperations();
    return gridFsOperations;
  }

  abstract DropwizardAppExtension<MyConfiguration> getDW();
}
