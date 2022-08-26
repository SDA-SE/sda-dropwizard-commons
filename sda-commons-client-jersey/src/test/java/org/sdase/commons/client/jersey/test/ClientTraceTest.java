package org.sdase.commons.client.jersey.test;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.noContent;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.http.RequestMethod.GET;
import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.newRequestPattern;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static javax.ws.rs.core.HttpHeaders.LOCATION;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import io.dropwizard.Application;
import io.dropwizard.forms.MultiPartBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.junit.DropwizardAppRule;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.List;
import javax.ws.rs.client.Client;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.rules.RuleChain;
import org.sdase.commons.client.jersey.JerseyClientBundle;
import org.sdase.commons.server.dropwizard.bundles.ConfigurationSubstitutionBundle;
import org.sdase.commons.server.jackson.JacksonConfigurationBundle;
import org.sdase.commons.server.testing.SystemPropertyRule;
import org.sdase.commons.server.trace.TraceTokenBundle;

public class ClientTraceTest {

  @ClassRule
  public static final WireMockClassRule WIRE =
      new WireMockClassRule(wireMockConfig().dynamicPort());

  private final DropwizardAppRule<ClientTestConfig> dw =
      new DropwizardAppRule<>(ClientTestApp.class, resourceFilePath("test-config.yaml"));

  @Rule
  public final RuleChain rule =
      RuleChain.outerRule(new SystemPropertyRule().setProperty("MOCK_BASE_URL", WIRE.baseUrl()))
          .around(dw);

  @RegisterExtension static final OpenTelemetryExtension OTEL = OpenTelemetryExtension.create();

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

    List<SpanData> spans = OTEL.getSpans();
    assertThat(spans).hasSize(1).first().extracting(SpanData::getName).isEqualTo("HTTP GET");

    assertThat(spans.get(0).getAttributes())
        .extracting(
            att -> att.get(SemanticAttributes.HTTP_URL),
            att -> att.get(SemanticAttributes.HTTP_METHOD))
        .contains(WIRE.baseUrl(), "GET");
  }

  @Test
  public void passTraceIdInHeaders() {
    Client client =
        app.getJerseyClientBundle().getClientFactory().externalClient().buildGenericClient("test");
    client.target(WIRE.baseUrl()).request().get();

    WIRE.verify(
        1, newRequestPattern(GET, urlEqualTo("/")).withHeader("traceparent", matching(".+")));
  }

  public static class ClientTestApp extends Application<ClientTestConfig> {

    private final JerseyClientBundle<ClientTestConfig> jerseyClientBundle =
        JerseyClientBundle.builder()
            .withConsumerTokenProvider(ClientTestConfig::getConsumerToken)
            .withTelemetryInstance(OTEL.getOpenTelemetry())
            .build();

    @Override
    public void initialize(Bootstrap<ClientTestConfig> bootstrap) {
      bootstrap.addBundle(ConfigurationSubstitutionBundle.builder().build());
      bootstrap.addBundle(JacksonConfigurationBundle.builder().build());
      bootstrap.addBundle(TraceTokenBundle.builder().build());
      bootstrap.addBundle(jerseyClientBundle);
      bootstrap.addBundle(new MultiPartBundle());
    }

    @Override
    public void run(ClientTestConfig configuration, Environment environment) {
      environment.jersey().register(this);
      environment
          .jersey()
          .register(
              new ClientTestEndPoint(
                  jerseyClientBundle.getClientFactory(), configuration.getMockBaseUrl()));
    }

    public JerseyClientBundle<ClientTestConfig> getJerseyClientBundle() {
      return jerseyClientBundle;
    }
  }
}
