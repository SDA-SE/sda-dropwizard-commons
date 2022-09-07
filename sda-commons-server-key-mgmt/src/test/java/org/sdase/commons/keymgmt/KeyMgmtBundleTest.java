package org.sdase.commons.keymgmt;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import org.assertj.core.groups.Tuple;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.sdase.commons.keymgmt.manager.KeyManager;
import org.sdase.commons.keymgmt.manager.KeyMapper;
import org.sdase.commons.keymgmt.manager.NoKeyKeyManager;
import org.sdase.commons.keymgmt.manager.PassthroughKeyMapper;
import org.sdase.commons.keymgmt.validator.PlatformKey;
import org.sdase.commons.keymgmt.validator.PlatformKeys;
import org.sdase.commons.shared.api.error.ApiError;
import org.sdase.commons.starter.SdaPlatformBundle;

class KeyMgmtBundleTest {

  @RegisterExtension
  @Order(0)
  static final DropwizardAppExtension<KeyMgmtBundleTestConfig> DW =
      new DropwizardAppExtension<>(
          KeyMgmtBundleTestApp.class,
          resourceFilePath("test-config.yml"),
          ConfigOverride.config("keyMgmt.apiKeysDefinitionPath", resourceFilePath("keys")),
          ConfigOverride.config("keyMgmt.mappingDefinitionPath", resourceFilePath("mappings")));

  private static KeyMgmtBundle<KeyMgmtBundleTestConfig> keyMgmtBundle;
  private static WebTarget client;

  @BeforeAll
  static void init() {
    KeyMgmtBundleTestApp app = DW.getApplication();
    keyMgmtBundle = app.getKeyMgmtBundle();
    assertThat(keyMgmtBundle).isNotNull();
    client = DW.client().target(String.format("http://localhost:%d", DW.getLocalPort()));
  }

  @Test
  void shouldGetNoKeyKeyManagerIfKeyDoesNotExist() {
    assertThat(keyMgmtBundle.createKeyManager("NOTEXISTING")).isInstanceOf(NoKeyKeyManager.class);
  }

  @Test
  void shouldGetPassthroughKeyMapperManagerIfMappingDoesNotExists() {
    assertThat(keyMgmtBundle.createKeyMapper("NOTEXISTING"))
        .isInstanceOf(PassthroughKeyMapper.class);
  }

  @Test
  void shouldGetKeyManagerManagerCaseInsensitive() {
    KeyManager upperCase = keyMgmtBundle.createKeyManager("GENDER");
    KeyManager lowerCase = keyMgmtBundle.createKeyManager("gender");
    KeyManager mixedCase = keyMgmtBundle.createKeyManager("GeNdER");
    assertThat(upperCase).isEqualTo(lowerCase).isEqualTo(mixedCase);
  }

  @Test
  void shouldGetKeyMappingCaseInsensitive() {
    KeyMapper upperCase = keyMgmtBundle.createKeyMapper("GENDER");
    KeyMapper lowerCase = keyMgmtBundle.createKeyMapper("gender");
    KeyMapper mixedCase = keyMgmtBundle.createKeyMapper("GeNdER");
    assertThat(upperCase).isEqualTo(lowerCase).isEqualTo(mixedCase);
  }

  @Test
  void shouldCheckKeyValuesCaseInsensitive() {
    KeyManager gender = keyMgmtBundle.createKeyManager("GENDER");
    assertThat(gender.isValidValue("MALE")).isTrue();
    assertThat(gender.isValidValue("Male")).isTrue();
    assertThat(gender.isValidValue("male")).isTrue();
  }

  @Test
  void shouldGetMappingValueToImpl() {
    KeyMapper gender = keyMgmtBundle.createKeyMapper("GENDER");
    assertThat(gender.toImpl("MALE")).isEqualTo("m");
    assertThat(gender.toImpl("male")).isEqualTo("m");
    assertThat(gender.toImpl("mAlE")).isEqualTo("m");
    assertThat(gender.toImpl("FeMaLe")).isEqualTo("F");
  }

  @Test
  void shouldGetMappingValueWithPrecedence() {
    assertThat(keyMgmtBundle.createKeyMapper("LINES_OF_BUSINESS").toImpl("HOUSEHOLD"))
        .isEqualTo("020");
  }

  @Test
  void shouldGetMappingValue() {
    assertThat(keyMgmtBundle.createKeyMapper("LINES_OF_BUSINESS").toImpl("CAR")).isEqualTo("001");
  }

  @Test
  void shouldGetMappingValueToApiCaseSensitive() {
    KeyMapper gender = keyMgmtBundle.createKeyMapper("GENDER");
    assertThat(gender.toApi("m")).isEqualTo("MALE");
    assertThat(gender.toApi("F")).isEqualTo("FEMALE");
    // pass through case
    assertThat(gender.toApi("f")).isEqualTo("f");
  }

  @Test
  void shouldGetMappingValueToApiPrecedence() {
    assertThat(keyMgmtBundle.createKeyMapper("SALUTATION").toApi("2")).isEqualTo("MRS");
  }

  @Test
  void shouldGetMappingValueToApi() {
    assertThat(keyMgmtBundle.createKeyMapper("SALUTATION").toApi("0")).isEqualTo("MR");
  }

  @Test
  void shouldConfirmValidKey() {
    assertThat(keyMgmtBundle.createKeyManager("GENDER").isValidValue("MALE")).isTrue();
    assertThat(keyMgmtBundle.createKeyManager("GENDER").isValidValue("MaLe")).isTrue();
    assertThat(keyMgmtBundle.createKeyManager("GENDER").isValidValue("male")).isTrue();
  }

  @Test
  void shouldDeclineInvalidKey() {
    assertThat(keyMgmtBundle.createKeyManager("GENDER").isValidValue("Mr")).isFalse();
  }

  @Test
  void shouldReturnListOfValidValues() {
    assertThat(keyMgmtBundle.createKeyManager("GENDER").getValidValues())
        .containsExactlyInAnyOrder("MALE", "FEMALE", "OTHER");
  }

  @Test
  void shouldReturnFalseIfKeyNotEmpty() {
    assertThat(keyMgmtBundle.createKeyManager("NONEXISTING").getValidValues()).isEmpty();
  }

  @Test
  void shouldMapBiDirectionalMappings() {
    assertThat(keyMgmtBundle.createKeyMapper("BIDIRECTIONAL_ONLY").toApi("A"))
        .isEqualTo("A"); // Pass through
    assertThat(keyMgmtBundle.createKeyMapper("BIDIRECTIONAL_ONLY").toImpl("B"))
        .isEqualTo("B"); // Pass through
    assertThat(keyMgmtBundle.createKeyMapper("BIDIRECTIONAL_ONLY").toImpl("A")).isEqualTo("B");
    assertThat(keyMgmtBundle.createKeyMapper("BIDIRECTIONAL_ONLY").toApi("B")).isEqualTo("A");
  }

  @Test
  void shouldGetAllKeys() {
    assertThat(keyMgmtBundle.getKeyDefinitionNames())
        .containsExactlyInAnyOrder("GENDER", "SALUTATION");
  }

  @Test
  void shouldGetAllMappings() {
    assertThat(keyMgmtBundle.getMappingKeyDefinitionNames())
        .containsExactlyInAnyOrder(
            "GENDER", "SALUTATION", "LINES_OF_BUSINESS", "BIDIRECTIONAL_ONLY");
  }

  @Test
  void shouldValidatePlatformKeySuccess() {
    try (Response response =
        client
            .path("api")
            .path("validate")
            .request()
            .post(Entity.entity(new ObjectWithKey().setGenderKey("MALE"), APPLICATION_JSON))) {
      assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT_204);
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"MALE", "MR"})
  void shouldValidatePlatformKeysSuccess(String input) {
    try (Response response =
        client
            .path("api")
            .path("validate")
            .request()
            .post(
                Entity.entity(
                    new ObjectWithKey().setGenderOrSalutationKey(input), APPLICATION_JSON))) {
      assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT_204);
    }
  }

  @ParameterizedTest
  @MethodSource("provideObjectWithInvalidKey")
  void shouldValidatePlatformKeyFail(ObjectWithKey objectWithKey, Tuple expectedMessageTuple) {
    try (Response response =
        client
            .path("api")
            .path("validate")
            .request()
            .post(Entity.entity(objectWithKey, APPLICATION_JSON))) {
      assertThat(response.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY_422);
      ApiError error = response.readEntity(ApiError.class);
      assertThat(error.getInvalidParams())
          .extracting("field", "reason", "errorCode")
          .contains(expectedMessageTuple);
    }
  }

  private static Stream<Arguments> provideObjectWithInvalidKey() {
    return Stream.of(
        Arguments.of(
            new ObjectWithKey().setGenderKey("BLA"),
            Tuple.tuple(
                "genderKey",
                "The attribute does not contain a valid platform key value.",
                "PLATFORM_KEY")),
        Arguments.of(
            new ObjectWithKey().setGenderKey("MALE").setGenderOrSalutationKey("BLA"),
            Tuple.tuple(
                "genderOrSalutationKey",
                "The attribute does not contain a valid platform key value.",
                "PLATFORM_KEYS")));
  }

  @Test
  void shouldValidatePlatformKeyNullSuccess() {
    try (Response response =
        client
            .path("api")
            .path("validate")
            .request()
            .post(Entity.entity(new ObjectWithKey().setGenderKey(null), APPLICATION_JSON))) {
      assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT_204);
    }
  }

  @Test
  void shouldValidateAsListElements() {
    try (Response response =
        client
            .path("api")
            .path("validate")
            .request()
            .post(
                Entity.entity(
                    new ObjectWithKey().setGenderList(Arrays.asList("MALE", "FEMALE")),
                    APPLICATION_JSON))) {
      assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT_204);
    }
  }

  @Test
  void shouldValidateAsListElementsFail() {
    try (Response response =
        client
            .path("api")
            .path("validate")
            .request()
            .post(
                Entity.entity(
                    new ObjectWithKey().setGenderList(Arrays.asList("MALE", "NOVALID")),
                    APPLICATION_JSON))) {
      assertThat(response.getStatus()).isEqualTo(422);
    }
  }

  @Test
  void shouldValidateAsParameter() {
    Response response =
        client
            .path("api")
            .path("validateParameter")
            .queryParam("param", "MALE")
            .request(APPLICATION_JSON)
            .get();
    assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT_204);
  }

  @Test
  void shouldValidateAsParameterFail() {
    Response responseNotValid =
        client
            .path("api")
            .path("validateParameter")
            .queryParam("param", "NOVALID")
            .request(APPLICATION_JSON)
            .get();
    assertThat(responseNotValid.getStatus()).isEqualTo(422);
  }

  public static class KeyMgmtBundleTestApp extends Application<KeyMgmtBundleTestConfig> {

    private final KeyMgmtBundle<KeyMgmtBundleTestConfig> keyMgmt =
        KeyMgmtBundle.builder()
            .withKeyMgmtConfigProvider(KeyMgmtBundleTestConfig::getKeyMgmt)
            .build();

    @Override
    public void initialize(Bootstrap<KeyMgmtBundleTestConfig> bootstrap) {
      bootstrap.addBundle(SdaPlatformBundle.builder().usingSdaPlatformConfiguration().build());
      bootstrap.addBundle(keyMgmt);
    }

    @Override
    public void run(KeyMgmtBundleTestConfig configuration, Environment environment) {
      environment.jersey().register(KeyMgmtBundleTestEndpoint.class);
    }

    public KeyMgmtBundle<KeyMgmtBundleTestConfig> getKeyMgmtBundle() {
      return keyMgmt;
    }
  }

  @Path("/")
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  public static class KeyMgmtBundleTestEndpoint {

    @POST
    @Path("/validate")
    public Response validate(@Valid ObjectWithKey valid) {
      return Response.noContent().build();
    }

    @GET
    @Path("/validateParameter")
    public Response validateParameter(@QueryParam("param") @PlatformKey("GENDER") String valid) {
      return Response.noContent().build();
    }
  }

  public static class ObjectWithKey {
    @PlatformKey("GENDER")
    private String genderKey;

    @PlatformKeys(values = {"GENDER", "SALUTATION"})
    private String genderOrSalutationKey;

    private List<@Valid @PlatformKey("GENDER") String> genderList;

    public String getGenderKey() {
      return genderKey;
    }

    public ObjectWithKey setGenderKey(String genderKey) {
      this.genderKey = genderKey;
      return this;
    }

    public String getGenderOrSalutationKey() {
      return genderOrSalutationKey;
    }

    public ObjectWithKey setGenderOrSalutationKey(String genderOrSalutationKey) {
      this.genderOrSalutationKey = genderOrSalutationKey;
      return this;
    }

    public List<String> getGenderList() {
      return genderList;
    }

    public ObjectWithKey setGenderList(List<String> genderList) {
      this.genderList = genderList;
      return this;
    }
  }
}
