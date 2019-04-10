package org.sdase.commons.server.opa;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.Lists;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import org.glassfish.jersey.client.ClientProperties;
import org.sdase.commons.server.opa.config.OpaConfig;
import org.sdase.commons.server.opa.config.OpaConfigProvider;
import org.sdase.commons.server.opa.filter.OpaAuthFilter;
import org.sdase.commons.server.opa.health.PolicyExistsHealthCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * The OPA bundle enables support for the Open Policy Agent
 * (http://openpolicyagent.org).
 * </p>
 * <p>
 * Note, the OPA bundle is not an alternative for
 * the @{@link org.sdase.commons.server.auth.AuthBundle} it is an addition for
 * authorization. The {@link org.sdase.commons.server.auth.AuthBundle} is still
 * required for validating the JWT
 * </p>
 * *
 * <p>
 * A new filter is added to the invocation chain of every endpoint invocation.
 * This filter invokes the OPA at the configured URL. Normally, this should be a
 * sidecar of the actual service. The response includes an authorization
 * decision and optional constraints that must be evaluated when querying the
 * database or filtering the result set of the request.
 * </p>
 * <p>
 * The constraints should be modeled as an Java pojo and documented within this
 * pojo. The OPA policies must be designed that the predefined result structure
 * is returned, such as
 * 
 * <pre>
 *     {@code
 *      {
 *         "result": {
 *            "allow": true,
 *            "constraints": { "constraint1": true, "constraint2": [ "v2.1", "v2.2" ] }
 *         }
 *      }
 *     }
 * </pre>
 * </p>
 * <p>
 * The filter evaluates the overall allow decision and adds the constraints to
 * the {@link javax.ws.rs.core.SecurityContext} as {@link OpaJwtPrincipal}.
 * </p>
 * <p>
 * The endpoints for swagger are excluded from the OPA filter.
 * </p>
 * 
 */
public class OpaBundle<T extends Configuration> implements ConfiguredBundle<T> {

   private static final Logger LOG = LoggerFactory.getLogger(OpaBundle.class);

   private final OpaConfigProvider<T> configProvider;

   private OpaBundle(OpaConfigProvider<T> configProvider) {
      this.configProvider = configProvider;
   }

   //
   // Builder
   //
   public static ProviderBuilder builder() {
      return new Builder<>();
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

      // create own object mapper to be independent from changes in jackson
      // bundle
      ObjectMapper objectMapper = createObjectMapper();

      // disable GZIP format since OPA cannot handle GZIP
      Client client = createClient(environment, config, objectMapper);

      WebTarget policyTarget = client.target(buildUrl(config));

      // exclude swagger
      List<String> excludePattern = new ArrayList<>();
      if (excludeSwagger()) {
         excludePattern.addAll(getSwaggerExcludePatterns());
      }

      // register filter
      environment.jersey().register(new OpaAuthFilter(policyTarget, config, excludePattern, objectMapper));

      // register health check
      if (!config.isDisableOpa()) {
         environment
             .healthChecks()
             .register(PolicyExistsHealthCheck.DEFAULT_NAME,
                 new PolicyExistsHealthCheck(policyTarget));
      }
   }

   private Client createClient(Environment environment, OpaConfig config, ObjectMapper objectMapper) {
      JerseyClientConfiguration configurationWithoutGzip = new JerseyClientConfiguration();
      configurationWithoutGzip.setGzipEnabled(false);
      configurationWithoutGzip.setGzipEnabledForRequests(false);

      Client client = new JerseyClientBuilder(environment)
            .using(configurationWithoutGzip)
            .using(objectMapper)
            .build("opaClient");

      // set read timeout if complex decisions are necessary
      client.property(ClientProperties.READ_TIMEOUT, config.getReadTimeout());
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
         if (getClass().getClassLoader().loadClass("org.sdase.commons.server.swagger.SwaggerBundle") != null) {
            return true;
         }
      } catch (ClassNotFoundException e) {
         // silently ignored
      }
      return false;
   }

   /**
    * builds the URL to the policiy
    * 
    * @param config
    *           OPA configuration
    * @return The complete policiy url
    */
   private String buildUrl(OpaConfig config) {
      return String.format("%s/v1/data/%s", config.getBaseUrl(), config.getPolicyPackagePath());
   }

   public interface ProviderBuilder {
      <C extends Configuration> OpaBuilder<C> withOpaConfigProvider(OpaConfigProvider<C> opaConfigProvider);
   }

   public interface OpaBuilder<C extends Configuration> {
      OpaBundle<C> build();
   }

   public static class Builder<C extends Configuration> implements ProviderBuilder, OpaBuilder<C> {

      private OpaConfigProvider<C> opaConfigProvider;

      private Builder() {
         // private method to prevent external instantiation
      }

      private Builder(OpaConfigProvider<C> opaConfigProvider) {
         this.opaConfigProvider = opaConfigProvider;
      }

      @Override
      public OpaBundle<C> build() {
         return new OpaBundle<>(opaConfigProvider);
      }

      @Override
      public <T extends Configuration> OpaBuilder<T> withOpaConfigProvider(OpaConfigProvider<T> opaConfigProvider) {
         return new Builder<>(opaConfigProvider);
      }
   }

}
