package org.sdase.commons.server.opa.testing;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import io.dropwizard.testing.junit.DropwizardAppRule;
import java.io.IOException;
import java.util.Map;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sdase.commons.server.auth.testing.AuthRule;
import org.sdase.commons.server.opa.testing.test.OpaBundeTestAppConfiguration;
import org.sdase.commons.server.opa.testing.test.OpaBundleTestApp;
import org.sdase.commons.server.testing.DropwizardRuleHelper;
import org.sdase.commons.server.testing.LazyRule;
import org.sdase.commons.server.testing.Retry;
import org.sdase.commons.server.testing.RetryRule;

public class OpaRequestsIT {

  private static final WireMockClassRule WIRE = new WireMockClassRule(
      wireMockConfig().dynamicPort());

  private static final AuthRule AUTH = AuthRule.builder().build();

  private static final LazyRule<DropwizardAppRule<OpaBundeTestAppConfiguration>> DW = new LazyRule<>(
      () -> DropwizardRuleHelper
          .dropwizardTestAppFrom(OpaBundleTestApp.class)
          .withConfigFrom(OpaBundeTestAppConfiguration::new)
          .withRandomPorts()
          .withConfigurationModifier(
              c -> c.getOpa().setBaseUrl(WIRE.baseUrl()).setPolicyPackage("my.policy")) // NOSONAR
          .withConfigurationModifier(AUTH.applyConfig(OpaBundeTestAppConfiguration::setAuth))
          .build());

  @ClassRule
  public static final RuleChain chain = RuleChain.outerRule(WIRE).around(AUTH).around(DW);
  @Rule
  public RetryRule retryRule = new RetryRule();

  @BeforeClass
  public static void start() {
    WIRE.start();
  }


  private void mock() {
    WIRE.stubFor(post("/v1/data/my/policy")
        .willReturn(aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody("{\n"
                + "  \"result\": {\n" // NOSONAR
                + "    \"allow\": true\n"
                + "  }\n"
                + "}")
        )
    );
  }

  @Before
  public void before() {
    WIRE.resetAll();
    mock();
  }

  @Test
  @Retry(5)
  public void shouldSerializePathAndMethodCorrectly() throws IOException {
    // when
    doGetRequest(null);

    // then
    String body = WIRE.getAllServeEvents().get(0).getRequest().getBodyAsString();
    Map<String, Object> opaInput = new ObjectMapper()
        .readValue(body, new TypeReference<Map<String, Object>>() {
        });

    basicAssertions(opaInput);
    assertThat(opaInput).extracting("input").extracting("trace").isNull(); // NOSONAR
    assertThat(opaInput).extracting("input").extracting("jwt").isNull();
  }



  @Test
  @Retry(5)
  public void shouldSerializeJwtCorrectly() throws IOException {
    // when
    MultivaluedMap<String, Object> headers = AUTH.auth().buildAuthHeader();
    doGetRequest(headers);

    // then
    String body = WIRE.getAllServeEvents().get(0).getRequest().getBodyAsString();
    Map<String, Object> opaInput = new ObjectMapper()
        .readValue(body, new TypeReference<Map<String, Object>>() {
        });

    basicAssertions(opaInput);
    assertThat(opaInput).extracting("input").extracting("trace").isNull();
    assertThat(opaInput).extracting("input").extracting("jwt")
        .isEqualTo(((String) headers.getFirst("Authorization")).substring("Bearer ".length()));
  }

  @Test
  @Retry(5)
  public void shouldSerializeTraceTokenCorrectly() throws IOException {
    // when
    MultivaluedMap<String, Object> headers = AUTH.auth().buildAuthHeader();
    headers.add("Trace-Token", "myTrace");
    doGetRequest(headers);

    // then
    String body = WIRE.getAllServeEvents().get(0).getRequest().getBodyAsString();
    Map<String, Object> opaInput = new ObjectMapper()
        .readValue(body, new TypeReference<Map<String, Object>>() {
        });

    basicAssertions(opaInput);
    assertThat(opaInput).extracting("input").extracting("trace").isEqualTo("myTrace");
  }


  @Test
  @Retry(5)
  public void shouldSerializeAdditionalHeaderCorrectly() throws IOException {
    // when
    MultivaluedMap<String, Object> headers = AUTH.auth().buildAuthHeader();
    headers.add("Simple", "SimpleValue");
    headers.add("Complex", "1");
    headers.add("Complex", "2");
    doGetRequest(headers);

    // then
    String body = WIRE.getAllServeEvents().get(0).getRequest().getBodyAsString();
    Map<String, Object> opaInput = new ObjectMapper()
        .readValue(body, new TypeReference<Map<String, Object>>() {
        });

    basicAssertions(opaInput);
    //noinspection unchecked,ConstantConditions
    Map<String, Object> extractedHeaders = (Map<String, Object>) ((Map<String, Object>) opaInput
        .get("input")).get("headers");
    assertThat(extractedHeaders).extracting("simple").asList().containsExactly("SimpleValue");
    assertThat(extractedHeaders).extracting("complex").asList().containsExactly("1,2");
  }

  private void doGetRequest(MultivaluedMap<String, Object> headers) {
    Response response = DW.getRule().client()
        .target("http://localhost:" + DW.getRule().getLocalPort()).path("resources").request()
        .headers(headers).get();

    assertThat(response.getStatus()).isEqualTo(200);
  }

  private void basicAssertions(Map<String, Object> opaInput) {
    assertThat(opaInput).extracting("input").extracting("httpMethod").isEqualTo("GET");
    assertThat(opaInput).extracting("input").extracting("path").asList()
        .containsExactly("resources");
  }

}
