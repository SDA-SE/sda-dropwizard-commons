package org.sdase.commons.server.spring.data.mongo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.of;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class MongoConfigurationUtilTest {

  @ParameterizedTest
  @MethodSource("validConfigurations")
  void shouldBuildConnectionString(MongoConfiguration given, String expected) {
    var actual = MongoConfigurationUtil.buildConnectionString(given);
    assertThat(actual).isEqualTo(expected);
  }

  static Stream<Arguments> validConfigurations() {
    return Stream.of(

        // variants of connection string are expected unchanged
        of(
            new MongoConfiguration()
                .setConnectionString("mongodb://mongodb.mongodb/database?authSource=admin"),
            "mongodb://mongodb.mongodb/database?authSource=admin"),
        of(
            new MongoConfiguration()
                .setConnectionString(
                    "mongodb://john:s3cr3t@mongodb.mongodb/database?authSource=admin"),
            "mongodb://john:s3cr3t@mongodb.mongodb/database?authSource=admin"),
        of(
            new MongoConfiguration()
                .setConnectionString(
                    "mongodb://john:s3cr3t@mongodb-0.mongodb:27017,mongodb-1.mongodb:27018,mongodb-2.mongodb:27019/database?authSource=admin"),
            "mongodb://john:s3cr3t@mongodb-0.mongodb:27017,mongodb-1.mongodb:27018,mongodb-2.mongodb:27019/database?authSource=admin"),

        // prefer connection string (allow invalid username/password when connection string is set)
        of(
            new MongoConfiguration()
                .setConnectionString("mongodb://mongodb.mongodb/database?authSource=admin")
                .setUsername("jane")
                .setPassword("s3cr3t")
                .setHosts("mongodb-dummy")
                .setDatabase("example")
                .setOptions("foo=bar"),
            "mongodb://mongodb.mongodb/database?authSource=admin"),
        of(
            new MongoConfiguration()
                .setConnectionString(
                    "mongodb://john:pr1v4t3@mongodb.mongodb/database?authSource=admin")
                .setPassword("s3cr3t")
                .setHosts("mongodb-dummy")
                .setDatabase("example")
                .setOptions("foo=bar"),
            "mongodb://john:pr1v4t3@mongodb.mongodb/database?authSource=admin"),
        of(
            new MongoConfiguration()
                .setConnectionString(
                    "mongodb://john:pr1v4t3@mongodb.mongodb/database?authSource=admin")
                .setUsername("jane")
                .setHosts("mongodb-dummy")
                .setDatabase("example")
                .setOptions("foo=bar"),
            "mongodb://john:pr1v4t3@mongodb.mongodb/database?authSource=admin"),

        // construct connection string if null
        of(
            new MongoConfiguration()
                .setConnectionString(null)
                .setUsername("jane")
                .setPassword("s3cr3t")
                .setHosts("mongodb.mongodb")
                .setDatabase("example")
                .setOptions("foo=bar"),
            "mongodb://jane:s3cr3t@mongodb.mongodb/example?foo=bar"),
        of(
            new MongoConfiguration()
                .setConnectionString(null)
                .setUsername("jane")
                .setPassword("s3cr3t")
                .setHosts("mongodb.mongodb")
                .setDatabase("example"),
            "mongodb://jane:s3cr3t@mongodb.mongodb/example"),
        of(
            new MongoConfiguration()
                .setConnectionString(null)
                .setHosts("mongodb.mongodb")
                .setDatabase("example")
                .setOptions("foo=bar"),
            "mongodb://mongodb.mongodb/example?foo=bar"),
        of(
            new MongoConfiguration()
                .setConnectionString(null)
                .setHosts("mongodb.mongodb")
                .setDatabase("example"),
            "mongodb://mongodb.mongodb/example"),
        of(
            new MongoConfiguration()
                .setConnectionString(null)
                .setHosts("mongodb-0.mongodb:27017,mongodb-1.mongodb:27018,mongodb-2.mongodb:27019")
                .setDatabase("example"),
            "mongodb://mongodb-0.mongodb:27017,mongodb-1.mongodb:27018,mongodb-2.mongodb:27019/example"),

        // construct connection string if blank
        of(
            new MongoConfiguration()
                .setConnectionString("  ")
                .setUsername("jane")
                .setPassword("s3cr3t")
                .setHosts("mongodb.mongodb")
                .setDatabase("example")
                .setOptions("foo=bar"),
            "mongodb://jane:s3cr3t@mongodb.mongodb/example?foo=bar"),
        of(
            new MongoConfiguration()
                .setConnectionString("  ")
                .setUsername("jane")
                .setPassword("s3cr3t")
                .setHosts("mongodb.mongodb")
                .setDatabase("example"),
            "mongodb://jane:s3cr3t@mongodb.mongodb/example"),
        of(
            new MongoConfiguration()
                .setConnectionString("  ")
                .setHosts("mongodb.mongodb")
                .setDatabase("example")
                .setOptions("foo=bar"),
            "mongodb://mongodb.mongodb/example?foo=bar"),
        of(
            new MongoConfiguration()
                .setConnectionString("  ")
                .setHosts("mongodb.mongodb")
                .setDatabase("example"),
            "mongodb://mongodb.mongodb/example"),
        of(
            new MongoConfiguration()
                .setConnectionString("  ")
                .setHosts("mongodb-0.mongodb:27017,mongodb-1.mongodb:27018,mongodb-2.mongodb:27019")
                .setDatabase("example"),
            "mongodb://mongodb-0.mongodb:27017,mongodb-1.mongodb:27018,mongodb-2.mongodb:27019/example"),

        // ignore if credentials are partially missing,
        // see
        // https://github.com/SDA-SE/sda-dropwizard-commons/blob/release/2.x.x/sda-commons-server-morphia/src/main/java/org/sdase/commons/server/morphia/internal/ConnectionStringUtil.java#L47
        of(
            new MongoConfiguration()
                .setUsername("john")
                .setHosts("mongodb.mongodb")
                .setDatabase("example"),
            "mongodb://mongodb.mongodb/example"),
        of(
            new MongoConfiguration()
                .setPassword("s3cr3t")
                .setHosts("mongodb.mongodb")
                .setDatabase("example"),
            "mongodb://mongodb.mongodb/example"));
  }
}
