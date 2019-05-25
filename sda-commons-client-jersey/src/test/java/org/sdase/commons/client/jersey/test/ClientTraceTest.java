package org.sdase.commons.client.jersey.test;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.noContent;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.http.RequestMethod.GET;
import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.newRequestPattern;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static io.opentracing.tag.Tags.COMPONENT;
import static io.opentracing.tag.Tags.HTTP_URL;
import static javax.ws.rs.core.HttpHeaders.LOCATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.sdase.commons.server.opentracing.filter.TagUtils.HTTP_REQUEST_HEADERS;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import io.dropwizard.testing.junit.DropwizardAppRule;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import javax.ws.rs.client.Client;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sdase.commons.server.testing.EnvironmentRule;

public class ClientTraceTest {

  @ClassRule
  public static final WireMockClassRule WIRE =
      new WireMockClassRule(wireMockConfig().dynamicPort());

  private final DropwizardAppRule<ClientTestConfig> dw =
      new DropwizardAppRule<>(ClientTestApp.class, resourceFilePath("test-config.yaml"));

  @Rule
  public final RuleChain rule =
      RuleChain.outerRule(new EnvironmentRule().setEnv("MOCK_BASE_URL", WIRE.baseUrl())).around(dw);

  private ClientTestApp app;

  @Before
  public void setUp() {
    WIRE.resetAll();
    WIRE.stubFor(get("/").willReturn(noContent()));
    app = dw.getApplication();
  }

  @Test
  public void traceClientRequests() {
    Client client =
        app.getJerseyClientBundle().getClientFactory().externalClient().buildGenericClient("test");
    client.target(WIRE.baseUrl()).request().header(LOCATION, "1").header(LOCATION, "2").get();

    MockTracer tracer = app.getTracer();
    assertThat(tracer.finishedSpans()).hasSize(1);
    MockSpan span = tracer.finishedSpans().get(0);
    assertThat(span.operationName()).isEqualTo("GET");
    assertThat(span.tags())
        .contains(
            entry(COMPONENT.getKey(), "jaxrs"),
            entry(HTTP_URL.getKey(), WIRE.baseUrl()),
            entry(HTTP_REQUEST_HEADERS.getKey(), "[Location = '1', '2']"));
  }

  @Test
  public void passTraceIdInHeaders() {
    Client client =
        app.getJerseyClientBundle().getClientFactory().externalClient().buildGenericClient("test");
    client.target(WIRE.baseUrl()).request().get();

    WIRE.verify(
        1,
        newRequestPattern(GET, urlEqualTo("/"))
            .withHeader("traceid", matching(".+"))
            .withHeader("spanid", matching(".+")));
  }
}
