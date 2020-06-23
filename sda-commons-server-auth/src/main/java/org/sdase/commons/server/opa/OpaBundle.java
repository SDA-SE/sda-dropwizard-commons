package org.sdase.commons.server.opa;

import static org.sdase.commons.server.opentracing.client.ClientTracingUtil.registerTracing;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.sdase.commons.server.opa.config.OpaConfig;
import org.sdase.commons.server.opa.config.OpaConfigProvider;
import org.sdase.commons.server.opa.extension.OpaInputExtension;
import org.sdase.commons.server.opa.extension.OpaInputHeadersExtension;
import org.sdase.commons.server.opa.filter.OpaAuthFilter;
import org.sdase.commons.server.opa.filter.model.OpaInput;
import org.sdase.commons.server.opa.health.PolicyExistsHealthCheck;
import org.sdase.commons.server.opa.internal.OpaJwtPrincipalFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The OPA bundle enables support for the Open Policy Agent (http://openpolicyagent.org).
 *
 * <p>Note, the OPA bundle is not an alternative for the @{@link
 * org.sdase.commons.server.auth.AuthBundle} it is an addition for authorization. The {@link
 * org.sdase.commons.server.auth.AuthBundle} is still required for validating the JWT *
 *
 * <p>A new filter is added to the invocation chain of every endpoint invocation. This filter
 * invokes the OPA at the configured URL. Normally, this should be a sidecar of the actual service.
 * The response includes an authorization decision and optional constraints that must be evaluated
 * when querying the database or filtering the result set of the request.
 *
 * <p>The constraints should be modeled as an Java pojo and documented within this pojo. The OPA
 * policies must be designed that the predefined result structure is returned, such as
 *
 * <pre>{@code
 * {
 *    "result": {
 *       "allow": true,
 *       "constraint1": true,
 *       "constraint2": [ "v2.1", "v2.2" ]
 *    }
 * }
 *
 * }</pre>
 *
 * <p>The filter evaluates the overall allow decision and adds the constraints to the {@link
 * javax.ws.rs.core.SecurityContext} as {@link OpaJwtPrincipal}.
 *
 * <p>The endpoints for swagger are excluded from the OPA filter.
 */
public class OpaBundle<T extends Configuration> implements ConfiguredBundle<T> {

  private static final Logger LOG = LoggerFactory.getLogger(OpaBundle.class);

  private final OpaConfigProvider<T> configProvider;
  private final Map<String, OpaInputExtension<?>> inputExtensions;
  private final Tracer tracer;

  private OpaBundle(
      OpaConfigProvider<T> configProvider,
      Map<String, OpaInputExtension<?>> inputExtensions,
      Tracer tracer) {
    this.configProvider = configProvider;
    this.inputExtensions = inputExtensions;
    this.tracer = tracer;
  }

  //
  // Builder
  //
  public static ProviderBuilder builder() {
    return new Builder<>();
  }

  @VisibleForTesting
  @SuppressWarnings("java:S1452") // allow generic wildcard type
  public Map<String, OpaInputExtension<?>> getInputExtensions() {
    return inputExtensions;
  }

  @Override
  public void initialize(Bootstrap<?> bootstrap) {
    // no init
  }

  @Override
  public void run(T configuration, Environment environment) {
    OpaConfig config = configProvider.apply(configuration);

    if (config.isDisableOpa()) {
      LOG.warn("Authorization is disabled. This setting should NEVER be used in production.");
    }

    Tracer currentTracer = tracer == null ? GlobalTracer.get() : tracer;

    // create own object mapper to be independent from changes in jackson
    // bundle
    ObjectMapper objectMapper = createObjectMapper();

    // disable GZIP format since OPA cannot handle GZIP
    Client client = createClient(environment, config, objectMapper, currentTracer);

    WebTarget policyTarget = client.target(buildUrl(config));

    // exclude swagger
    List<String> excludePattern = new ArrayList<>();
    if (excludeSwagger()) {
      excludePattern.addAll(getSwaggerExcludePatterns());
    }

    // register filter
    environment
        .jersey()
        .register(
            new OpaAuthFilter(
                policyTarget,
                config,
                excludePattern,
                objectMapper,
                inputExtensions,
                currentTracer));

    // register health check
    if (!config.isDisableOpa()) {
      environment
          .healthChecks()
          .register(
              PolicyExistsHealthCheck.DEFAULT_NAME, new PolicyExistsHealthCheck(policyTarget));
    }

    environment
        .jersey()
        .register(
            new AbstractBinder() {
              @Override
              protected void configure() {
                bindFactory(OpaJwtPrincipalFactory.class)
                    .to(OpaJwtPrincipal.class)
                    .proxy(true)
                    .proxyForSameScope(true)
                    .in(RequestScoped.class);
              }
            });
  }

  private Client createClient(
      Environment environment, OpaConfig config, ObjectMapper objectMapper, Tracer tracer) {
    JerseyClientConfiguration configurationWithoutGzip = new JerseyClientConfiguration();
    configurationWithoutGzip.setGzipEnabled(false);
    configurationWithoutGzip.setGzipEnabledForRequests(false);

    Client client =
        new JerseyClientBuilder(environment)
            .using(configurationWithoutGzip)
            .using(objectMapper)
            .build("opaClient");

    // set read timeout if complex decisions are necessary
    client.property(ClientProperties.READ_TIMEOUT, config.getReadTimeout());

    registerTracing(client, tracer);

    return client;
  }

  private ObjectMapper createObjectMapper() {
    ObjectMapper objectMapper = Jackson.newObjectMapper();
    objectMapper
        // serialization
        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        // deserialization
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES)
        .disable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE)
        .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
    return objectMapper;
  }

  private List<String> getSwaggerExcludePatterns() {
    return Lists.newArrayList("swagger\\.(json|yaml)");
  }

  private boolean excludeSwagger() {
    try {
      if (getClass().getClassLoader().loadClass("org.sdase.commons.server.swagger.SwaggerBundle")
          != null) {
        return true;
      }
    } catch (ClassNotFoundException e) {
      // silently ignored
    }
    return false;
  }

  /**
   * builds the URL to the policy
   *
   * @param config OPA configuration
   * @return The complete policy url
   */
  private String buildUrl(OpaConfig config) {
    return String.format("%s/v1/data/%s", config.getBaseUrl(), config.getPolicyPackagePath());
  }

  public interface ProviderBuilder {
    <C extends Configuration> OpaExtensionsBuilder<C> withOpaConfigProvider(
        OpaConfigProvider<C> opaConfigProvider);
  }

  public interface OpaExtensionsBuilder<C extends Configuration> extends OpaBuilder<C> {
    /**
     * Disable the {@link OpaInputHeadersExtension} to <i>not</i> forward any headers to the Open
     * Policy Agent.
     *
     * @return the buider
     */
    OpaExtensionsBuilder<C> withoutHeadersExtension();
  }

  public interface OpaBuilder<C extends Configuration> {

    /**
     * Register a custom {@link OpaInputExtension} that enriches the default {@link OpaInput} with
     * custom properties that are sent to the Open Policy Agent. Prefer authorization based on the
     * {@link OpaJwtPrincipal#getConstraintsAsEntity(Class) constraints} that are returned after
     * policy execution.
     *
     * <p>Please note that it is prohibited to override properties that are already set in the
     * original {@link org.sdase.commons.server.opa.filter.model.OpaInput}.
     *
     * @param namespace the namespace is used as property name that the input should be accessible
     *     as in the OPA policy
     * @param extension the extension to register
     * @param <T> the type of data that is added to the input
     * @throws HiddenOriginalPropertyException thrown if the namespace of an input extension
     *     interferes with a property name of the original {@link OpaInput}.
     * @throws DuplicatePropertyException thrown if a namespace interferes with an already
     *     registered extension.
     * @return the builder
     */
    <T> OpaBuilder<C> withInputExtension(String namespace, OpaInputExtension<T> extension);

    OpaBuilder<C> withTracer(Tracer tracer);

    OpaBundle<C> build();
  }

  public static class Builder<C extends Configuration>
      implements ProviderBuilder, OpaExtensionsBuilder<C>, OpaBuilder<C> {

    private OpaConfigProvider<C> opaConfigProvider;
    private Tracer tracer;
    private final Map<String, OpaInputExtension<?>> inputExtensions = new HashMap<>();
    private boolean addHeadersExtension = true; // on by default

    private Builder() {
      // private method to prevent external instantiation
    }

    private Builder(OpaConfigProvider<C> opaConfigProvider) {
      this.opaConfigProvider = opaConfigProvider;
    }

    @Override
    public <T extends Configuration> OpaExtensionsBuilder<T> withOpaConfigProvider(
        OpaConfigProvider<T> opaConfigProvider) {
      return new Builder<>(opaConfigProvider);
    }

    @Override
    public OpaExtensionsBuilder<C> withoutHeadersExtension() {
      this.addHeadersExtension = false;
      return this;
    }

    @Override
    public <T> OpaBuilder<C> withInputExtension(String namespace, OpaInputExtension<T> extension) {
      // Check if the namespace of an extension would override any original field of the OpaInput.
      if (Arrays.stream(OpaInput.class.getDeclaredFields())
          .anyMatch(f -> f.getName().equals(namespace))) {
        throw new HiddenOriginalPropertyException(namespace, extension);
      }

      // Check if the namespace has already been registered before
      if (inputExtensions.containsKey(namespace)) {
        throw new DuplicatePropertyException(namespace, extension, inputExtensions.get(namespace));
      }

      this.inputExtensions.put(namespace, extension);
      return this;
    }

    @Override
    public OpaBuilder<C> withTracer(Tracer tracer) {
      this.tracer = tracer;
      return this;
    }

    @Override
    public OpaBundle<C> build() {
      if (addHeadersExtension) {
        withInputExtension("headers", OpaInputHeadersExtension.builder().build());
      }

      return new OpaBundle<>(opaConfigProvider, inputExtensions, tracer);
    }
  }

  public static class HiddenOriginalPropertyException extends RuntimeException {
    public HiddenOriginalPropertyException(String namespace, OpaInputExtension<?> extension) {
      super(
          String.format(
              "The extension \"%s\" would override the original field \"%s\" of the OpaInput. This is not allowed!",
              extension.getClass().getName(), namespace));
    }
  }

  public static class DuplicatePropertyException extends RuntimeException {
    public DuplicatePropertyException(
        String namespace,
        OpaInputExtension<?> extension,
        OpaInputExtension<?> alreadyRegisteredExtension) {
      super(
          String.format(
              "There is already an extension \"%s\" registered for the field \"%s\". The extension \"%s\" would override this field.",
              alreadyRegisteredExtension.getClass().getName(),
              namespace,
              extension.getClass().getName()));
    }
  }
}
