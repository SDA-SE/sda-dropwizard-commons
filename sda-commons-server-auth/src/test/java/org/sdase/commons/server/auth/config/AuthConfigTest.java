package org.sdase.commons.server.auth.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.testing.ResourceHelpers;
import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.ClassRule;
import org.junit.Test;
import org.sdase.commons.server.testing.EnvironmentRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthConfigTest {

  private static final Logger LOG = LoggerFactory.getLogger(AuthConfigTest.class);

  private static String KEYS_JSON = initKeys();

  private static String initKeys() {
    List<KeyLocation> keys = new ArrayList<>();
    KeyLocation pem = new KeyLocation();
    String pemLocation = ResourceHelpers.resourceFilePath("example.pem");
    pem.setLocation(new File(pemLocation).toURI());
    pem.setType(KeyUriType.PEM);
    pem.setPemKeyId("example");
    keys.add(pem);
    KeyLocation discovery = new KeyLocation();
    discovery.setLocation(URI.create("http://keycloak.example.com/auth/realms/my-app"));
    discovery.setType(KeyUriType.OPEN_ID_DISCOVERY);
    keys.add(discovery);
    try {
      String keysJson = new ObjectMapper().writeValueAsString(keys);
      LOG.info("PEM location: {}", pemLocation);
      LOG.info("Created keys for environment: {}", keysJson);
      return keysJson;
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  @ClassRule public static EnvironmentRule ENV = new EnvironmentRule().setEnv("KEYS", KEYS_JSON);

  @Test
  public void shouldReadConfigFromJsonEnvironment() throws Exception {
    String configPath = "test-config.yaml";
    String config = readConfigWithSubstitution(configPath);
    TestConfig testConfig =
        new ObjectMapper(new YAMLFactory())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .readValue(config, TestConfig.class);
    assertThat(testConfig).isNotNull();
    assertThat(testConfig.getAuth()).isNotNull();
    assertThat(testConfig.getAuth().getKeys())
        .extracting(KeyLocation::getType, KeyLocation::getLocation, KeyLocation::getPemKeyId)
        .containsExactly(
            tuple(
                KeyUriType.PEM,
                new File(ResourceHelpers.resourceFilePath("example.pem")).toURI(),
                "example"),
            tuple(
                KeyUriType.OPEN_ID_DISCOVERY,
                URI.create("http://keycloak.example.com/auth/realms/my-app"),
                null));
  }

  private String readConfigWithSubstitution(String configPath) throws IOException {
    SubstitutingSourceProvider sourceProvider =
        new SubstitutingSourceProvider(
            FileInputStream::new, new EnvironmentVariableSubstitutor(false));
    String config;
    try (BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(
                sourceProvider.open(ResourceHelpers.resourceFilePath(configPath))))) {
      config = reader.lines().collect(Collectors.joining(System.lineSeparator()));
    }
    return config;
  }

  public static class TestConfig {
    private AuthConfig auth;

    public AuthConfig getAuth() {
      return auth;
    }

    public TestConfig setAuth(AuthConfig auth) {
      this.auth = auth;
      return this;
    }
  }
}
