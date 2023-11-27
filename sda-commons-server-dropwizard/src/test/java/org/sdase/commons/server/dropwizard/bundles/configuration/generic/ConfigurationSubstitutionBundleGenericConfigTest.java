package org.sdase.commons.server.dropwizard.bundles.configuration.generic;

import static io.dropwizard.testing.ConfigOverride.randomPorts;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.params.provider.Arguments.of;
import static org.sdase.commons.server.auth.config.KeyUriType.JWKS;
import static org.sdase.commons.server.auth.config.KeyUriType.OPEN_ID_DISCOVERY;
import static org.sdase.commons.server.auth.config.KeyUriType.PEM;

import com.codahale.metrics.annotation.ResponseMeteredLevel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TextNode;
import io.dropwizard.core.Application;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.util.DataSize;
import io.dropwizard.util.Duration;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junitpioneer.jupiter.SetSystemProperty;
import org.sdase.commons.server.auth.config.AuthConfig;
import org.sdase.commons.server.dropwizard.bundles.ConfigurationSubstitutionBundle;
import org.sdase.commons.server.kafka.KafkaConfiguration;
import org.slf4j.LoggerFactory;

class ConfigurationSubstitutionBundleGenericConfigTest {

  static final JsonNodeFactory JSON_NODE_FACTORY = new ObjectMapper().getNodeFactory();

  @MethodSource
  @ParameterizedTest
  void shouldSetCommonValues(
      String givenKey, String givenValue, List<String> expectedPath, Object expectedValue)
      throws Throwable {
    try {
      System.setProperty(givenKey, givenValue);
      startAppToTestConfiguration(c -> assertValueInJavaObjectPath(c, expectedPath, expectedValue));

    } finally {
      System.clearProperty(givenKey);
    }
  }

  @Test
  @SetSystemProperty(key = "KAFKA_BROKERS_3", value = "kafka-4.example.com")
  @SetSystemProperty(key = "KAFKA_BROKERS_4", value = "kafka-5.example.com")
  @SetSystemProperty(key = "KAFKA_BROKERS_5", value = "kafka-6.example.com")
  @SetSystemProperty(key = "KAFKA_BROKERS_6", value = "kafka-7.example.com")
  @SetSystemProperty(key = "KAFKA_BROKERS_7", value = "kafka-8.example.com")
  @SetSystemProperty(key = "KAFKA_BROKERS_8", value = "kafka-9.example.com")
  @SetSystemProperty(key = "KAFKA_BROKERS_9", value = "kafka-10.example.com")
  @SetSystemProperty(key = "KAFKA_BROKERS_10", value = "kafka-11.example.com")
  @SetSystemProperty(key = "KAFKA_BROKERS_0", value = "kafka-1.example.com")
  @SetSystemProperty(key = "KAFKA_BROKERS_1", value = "kafka-2.example.com")
  @SetSystemProperty(key = "KAFKA_BROKERS_2", value = "kafka-3.example.com")
  void shouldSetMultipleArrayValues() throws Throwable {
    startAppToTestConfiguration(
        c ->
            assertValueInJavaObjectPath(
                c,
                List.of("kafka", "brokers"),
                List.of(
                    "kafka-1.example.com",
                    "kafka-2.example.com",
                    "kafka-3.example.com",
                    "kafka-4.example.com",
                    "kafka-5.example.com",
                    "kafka-6.example.com",
                    "kafka-7.example.com",
                    "kafka-8.example.com",
                    "kafka-9.example.com",
                    "kafka-10.example.com",
                    "kafka-11.example.com")));
  }

  @Test
  @SetSystemProperty(key = "AUTH_KEYS_1_LOCATION", value = "https://login.com")
  @SetSystemProperty(key = "AUTH_KEYS_1_REQUIREDISSUER", value = "https://login.com")
  @SetSystemProperty(key = "AUTH_KEYS_1_TYPE", value = "OPEN_ID_DISCOVERY")
  @SetSystemProperty(key = "AUTH_KEYS_2_LOCATION", value = "https://idp.com/jwks.json")
  @SetSystemProperty(key = "AUTH_KEYS_2_REQUIREDISSUER", value = "https://idp.com")
  @SetSystemProperty(key = "AUTH_KEYS_2_TYPE", value = "JWKS")
  @SetSystemProperty(key = "AUTH_KEYS_0_LOCATION", value = "file://internal.pem")
  @SetSystemProperty(key = "AUTH_KEYS_0_TYPE", value = "PEM")
  @SetSystemProperty(key = "AUTH_KEYS_0_PEMKEYID", value = "internal-abcd")
  @SetSystemProperty(key = "AUTH_KEYS_0_PEMSIGNALG", value = "ES512")
  void shouldDefineMultipleAuthIssuersWithEnvs() throws Throwable {
    startAppToTestConfiguration(
        c ->
            assertThat(c)
                .extracting(TestConfiguration.class::cast)
                .extracting(TestConfiguration::getAuth)
                .extracting(AuthConfig::getKeys)
                .asList()
                .extracting("location", "type", "pemKeyId", "pemSignAlg", "requiredIssuer")
                .containsExactly(
                    tuple(URI.create("file://internal.pem"), PEM, "internal-abcd", "ES512", null),
                    tuple(
                        URI.create("https://login.com"),
                        OPEN_ID_DISCOVERY,
                        null,
                        null,
                        "https://login.com"),
                    tuple(
                        URI.create("https://idp.com/jwks.json"),
                        JWKS,
                        null,
                        null,
                        "https://idp.com")));
  }

  @Test
  @SetSystemProperty(key = "LOGGING_LOGGERS_com.sdase.cars.CarsApp", value = "TRACE")
  @SetSystemProperty(key = "LOGGING_LOGGERS_com.sdase.drivers.DriversApp_level", value = "DEBUG")
  void shouldSetLogLevels() throws Throwable {
    startAppToTestConfiguration(
        c ->
            assertSoftly(
                softly -> {
                  var carsLogger = LoggerFactory.getLogger("com.sdase.cars.CarsApp");
                  softly.assertThat(carsLogger.isTraceEnabled()).isTrue();
                  var driversLogger = LoggerFactory.getLogger("com.sdase.drivers.DriversApp");
                  softly.assertThat(driversLogger.isTraceEnabled()).isFalse();
                  softly.assertThat(driversLogger.isDebugEnabled()).isTrue();
                }));
  }

  @Test
  @SetSystemProperty(key = "KAFKA_CONFIG_SOME_CONFIG", value = "foo")
  void shouldIgnoreEnvsDefinedInConfigYaml() throws Exception {
    var testSupport =
        new DropwizardTestSupport<>(
            TestApp.class, resourceFilePath("config-with-properties.yaml"), randomPorts());
    try {
      testSupport.before();
      TestApp application = testSupport.getApplication();
      TestConfiguration testConfiguration = (TestConfiguration) application.getConfiguration();
      assertThat(testConfiguration.getKafka().getConfig())
          .containsExactly(entry("some.config", "foo"));
    } finally {
      testSupport.after();
    }
  }

  void assertValueInJavaObjectPath(
      Object actualConfiguration, List<String> expectedPath, Object expectedValue) {
    AbstractObjectAssert<?, ?> configAssert = assertThat(actualConfiguration);
    for (var pathSegment : expectedPath) {
      configAssert = configAssert.extracting(pathSegment);
    }
    configAssert.isEqualTo(expectedValue);
  }

  static Stream<Arguments> shouldSetCommonValues() {
    return Stream.of(
        of("CHANGEDNAME", "Hello World!", List.of("originalName"), "Hello World!"),
        of("UNCOMMON_JAVA_NAME", "Hello World!", List.of("uncommon_java_name"), "Hello World!"),
        of("JSONNODE", "Hello!", List.of("jsonNode"), new TextNode("Hello!")),
        of(
            "JSONNODE_0",
            "Hello",
            List.of("jsonNode"),
            JSON_NODE_FACTORY.arrayNode().add(new TextNode("Hello"))),
        of(
            "JSONNODE_foo",
            "bar",
            List.of("jsonNode"),
            JSON_NODE_FACTORY.objectNode().put("foo", "bar")),
        of(
            "JSONNODE_foo_0_bar",
            "Hello",
            List.of("jsonNode"),
            JSON_NODE_FACTORY
                .objectNode()
                .set(
                    "foo",
                    JSON_NODE_FACTORY
                        .arrayNode()
                        .add(JSON_NODE_FACTORY.objectNode().put("bar", "Hello")))),
        of("SERVER_GZIP_ENABLED", "true", List.of("server", "gzip", "enabled"), true),
        of("SERVER_GZIP_ENABLED", "false", List.of("server", "gzip", "enabled"), false),
        of(
            "SERVER_GZIP_BUFFERSIZE",
            "2mib",
            List.of("server", "gzip", "bufferSize"),
            DataSize.mebibytes(2)),
        of(
            "SERVER_SHUTDOWNGRACEPERIOD",
            "50s",
            List.of("server", "shutdownGracePeriod"),
            Duration.seconds(50)),
        of(
            "SERVER_RESPONSEMETEREDLEVEL",
            "ALL",
            List.of("server", "responseMeteredLevel"),
            ResponseMeteredLevel.ALL),
        of(
            "SERVER_APPLICATIONCONTEXTPATH",
            "/new-api",
            List.of("server", "applicationContextPath"),
            "/new-api"),
        of(
            "LISTOFLISTS_0_0",
            "Hello World!",
            List.of("listOfLists"),
            List.of(List.of("Hello World!"))),
        of("LISTOFMAPS_0_foo", "bar", List.of("listOfMaps"), List.of(Map.of("foo", "bar"))),
        of("MAPSTRINGSTRING_foo", "bar", List.of("mapStringString", "foo"), "bar"),
        of(
            "MAPSTRINGSTRING_kafka.config.trust.path",
            "/bundle.pem",
            List.of("mapStringString"),
            Map.of("kafka.config.trust.path", "/bundle.pem")),
        of(
            "MAPSTRINGSTRINGINITIALIZED_kafka.config.trust.path",
            "/bundle.pem",
            List.of("mapStringStringInitialized"),
            Map.of("kafka.config.trust.path", "/bundle.pem")),
        of(
            "MAPSTRINGSTRING_kafka-topic-foo",
            "events-everywhere",
            List.of("mapStringString", "kafka-topic-foo"),
            "events-everywhere"),
        of(
            "KAFKA_PRODUCERS_fooSender_CONFIG_auth",
            "admin",
            List.of("kafka", "producers", "fooSender", "config", "auth"),
            "admin"),
        of(
            "KAFKA_PRODUCERS_fooSender_CLIENTID",
            "i-am-unique",
            List.of("kafka", "producers", "fooSender", "clientId"),
            "i-am-unique"),
        of(
            "AUTH_KEYLOADERCLIENT_PROXY_NONPROXYHOSTS_0",
            "www.example.com",
            List.of("auth", "keyLoaderClient", "proxyConfiguration", "nonProxyHosts"),
            List.of("www.example.com"))
        // force line break
        );
  }

  void startAppToTestConfiguration(ThrowingConsumer<Configuration> assertions) throws Throwable {
    var testSupport = new DropwizardTestSupport<>(TestApp.class, null, randomPorts());
    try {
      testSupport.before();
      TestApp application = testSupport.getApplication();
      assertions.acceptThrows(application.getConfiguration());
    } finally {
      testSupport.after();
    }
  }

  public static class TestApp extends Application<TestConfiguration> {

    private Configuration configuration;

    @Override
    public void initialize(Bootstrap<TestConfiguration> bootstrap) {
      super.initialize(bootstrap);
      bootstrap.addBundle(ConfigurationSubstitutionBundle.builder().build());
    }

    @Override
    public void run(TestConfiguration configuration, Environment environment) {
      this.configuration = configuration;
    }

    public Configuration getConfiguration() {
      return configuration;
    }
  }

  @SuppressWarnings("unused")
  public static class TestConfiguration extends Configuration {
    @JsonProperty("changedName")
    private String originalName;

    private AuthConfig auth = new AuthConfig();

    private KafkaConfiguration kafka;

    @NotNull private String forTestingCommandOnly = "foo";

    private Map<String, String> mapStringString;

    private Map<String, String> mapStringStringInitialized = new LinkedHashMap<>();

    private RecursiveDesaster recursive = new RecursiveDesaster();

    private List<List<String>> listOfLists;

    private List<Map<String, String>> listOfMaps;

    private String uncommon_java_name;

    private JsonNode jsonNode;

    public String getOriginalName() {
      return originalName;
    }

    public TestConfiguration setOriginalName(String originalName) {
      this.originalName = originalName;
      return this;
    }

    public AuthConfig getAuth() {
      return auth;
    }

    public TestConfiguration setAuth(AuthConfig auth) {
      this.auth = auth;
      return this;
    }

    public KafkaConfiguration getKafka() {
      return kafka;
    }

    public TestConfiguration setKafka(KafkaConfiguration kafka) {
      this.kafka = kafka;
      return this;
    }

    public String getForTestingCommandOnly() {
      return forTestingCommandOnly;
    }

    public TestConfiguration setForTestingCommandOnly(String forTestingCommandOnly) {
      this.forTestingCommandOnly = forTestingCommandOnly;
      return this;
    }

    public Map<String, String> getMapStringString() {
      return mapStringString;
    }

    public TestConfiguration setMapStringString(Map<String, String> mapStringString) {
      this.mapStringString = mapStringString;
      return this;
    }

    public Map<String, String> getMapStringStringInitialized() {
      return mapStringStringInitialized;
    }

    public TestConfiguration setMapStringStringInitialized(
        Map<String, String> mapStringStringInitialized) {
      this.mapStringStringInitialized = mapStringStringInitialized;
      return this;
    }

    public RecursiveDesaster getRecursive() {
      return recursive;
    }

    public TestConfiguration setRecursive(RecursiveDesaster recursive) {
      this.recursive = recursive;
      return this;
    }

    public List<List<String>> getListOfLists() {
      return listOfLists;
    }

    public TestConfiguration setListOfLists(List<List<String>> listOfLists) {
      this.listOfLists = listOfLists;
      return this;
    }

    public List<Map<String, String>> getListOfMaps() {
      return listOfMaps;
    }

    public TestConfiguration setListOfMaps(List<Map<String, String>> listOfMaps) {
      this.listOfMaps = listOfMaps;
      return this;
    }

    public String getUncommon_java_name() {
      return uncommon_java_name;
    }

    public TestConfiguration setUncommon_java_name(String uncommon_java_name) {
      this.uncommon_java_name = uncommon_java_name;
      return this;
    }

    public JsonNode getJsonNode() {
      return jsonNode;
    }

    public TestConfiguration setJsonNode(JsonNode jsonNode) {
      this.jsonNode = jsonNode;
      return this;
    }
  }

  @SuppressWarnings("unused")
  public static class RecursiveDesaster {

    private Map<String, RecursiveDesaster> child = new HashMap<>();

    public Map<String, RecursiveDesaster> getChild() {
      return child;
    }

    public RecursiveDesaster setChild(Map<String, RecursiveDesaster> child) {
      this.child = child;
      return this;
    }
  }
}
