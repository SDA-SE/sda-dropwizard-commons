package org.sdase.commons.server.openapi;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.Validate.notBlank;
import static org.sdase.commons.server.dropwizard.lifecycle.ManagedShutdownListener.onShutdown;

import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.server.AbstractServerFactory;
import io.dropwizard.server.ServerFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import java.lang.invoke.MethodHandles;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.sdase.commons.optional.server.openapi.parameter.embed.EmbedParameterModifier;
import org.sdase.commons.server.openapi.filter.OpenAPISpecFilterSet;
import org.sdase.commons.server.openapi.filter.ServerUrlFilter;
import org.sdase.commons.server.openapi.hal.HALModelResolver;
import org.sdase.commons.server.openapi.hal.HalLinkDescriptionModifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A bundle used to generate and serve code-first OpenAPI 3 files at the {@code openapi.yaml} or
 * {@code openapi.json} HTTP resources.
 *
 * <h3>Example Usage</h3>
 *
 * <h4>Minimal</h4>
 *
 * <pre><code>
 *  &#64;OpenAPIDefinition(info = @Info(title = "An example application"))
 *  public class ExampleApplication extends Application&lt;Configuration&gt; {
 *
 *    // ...
 *
 *    &#64;Override
 *    public void initialize(Bootstrap&lt;Configuration&gt; bootstrap) {
 *      // ...
 *      bootstrap.addBundle(
 *        OpenApiBundle.builder()
 *          .addResourcePackageClass(getClass())
 *          .build());
 *    }
 *  }
 * </code></pre>
 */
public final class OpenApiBundle implements ConfiguredBundle<Configuration> {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final AtomicInteger UNIQUE_ID_COUNTER = new AtomicInteger();

  // https://www.dropwizard.io/en/release-2.0.x/manual/configuration.html
  private static final String DROPWIZARD_DEFAULT_ROOT_PATH = "/*";

  private final Set<String> resourcePackages;

  public static InitialBuilder builder() {
    return new Builder();
  }

  public OpenApiBundle(Set<String> resourcePackages) {
    this.resourcePackages = resourcePackages;
  }

  @Override
  public void initialize(Bootstrap<?> bootstrap) {
    // no initialization needed
  }

  @Override
  public void run(Configuration configuration, Environment environment) {
    // Get a new ID to register the openapi file in a unique context that is
    // not reused by another instance of this class. The context is registered on the first request
    // to {@link OpenApiResource} (or {@link DelegatingOpenApiResource}).
    String instanceId = Integer.toString(UNIQUE_ID_COUNTER.incrementAndGet());

    // Register a filter that adds a correct server url
    ServerUrlFilter serverUrlFilter = new ServerUrlFilter();
    environment.jersey().register(serverUrlFilter);
    OpenAPISpecFilterSet.register(serverUrlFilter);
    environment.lifecycle().manage(onShutdown(OpenAPISpecFilterSet::clear));

    // Add HAL support
    HALModelResolver halModelResolver = new HALModelResolver(environment.getObjectMapper());
    ModelConverters.getInstance().addConverter(halModelResolver);
    environment
        .lifecycle()
        .manage(onShutdown(() -> ModelConverters.getInstance().removeConverter(halModelResolver)));

    // Configure the OpenAPIConfiguration
    SwaggerConfiguration oasConfig =
        new SwaggerConfiguration()
            .openAPI(new OpenAPI())
            .prettyPrint(true)
            .resourcePackages(resourcePackages)
            .readAllResources(false)
            .filterClass(OpenAPISpecFilterSet.class.getName());

    // Register the resource that handles the openapi.{json|yaml} requests
    environment
        .jersey()
        .register(new DelegatingOpenApiResource(instanceId).openApiConfiguration(oasConfig));

    // Allow CORS to access (via wildcard) from Swagger UI/editor
    String basePath = determineBasePath(configuration);

    String filterBasePath = basePath.endsWith("/") ? basePath : basePath + "/"; // NOSONAR
    FilterRegistration.Dynamic filter =
        environment.servlets().addFilter("CORS OpenAPI", CrossOriginFilter.class);
    filter.addMappingForUrlPatterns(
        EnumSet.allOf(DispatcherType.class),
        true,
        filterBasePath + "openapi.yaml",
        filterBasePath + "openapi.json");
    filter.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
    filter.setInitParameter(CrossOriginFilter.ALLOW_CREDENTIALS_PARAM, Boolean.TRUE.toString());
    filter.setInitParameter(CrossOriginFilter.CHAIN_PREFLIGHT_PARAM, Boolean.FALSE.toString());

    LOG.info(
        "Initialized OpenAPI with base path '{}' and resource packages: '{}'",
        basePath,
        resourcePackages);
  }

  private String determineBasePath(Configuration configuration) {
    ServerFactory serverFactory = configuration.getServerFactory();

    if (serverFactory instanceof AbstractServerFactory) {
      String basePath =
          ((AbstractServerFactory) serverFactory)
              .getJerseyRootPath()
              .orElse(DROPWIZARD_DEFAULT_ROOT_PATH);

      // fix base path by removing '/*' at the end so swagger-ui can resolve it
      return "/*".equals(basePath) ? "/" : basePath.replaceAll("^(.*)/\\*$", "$1");
    }

    return "/api";
  }

  public interface InitialBuilder {

    /**
     * Adds a package to the packages Swagger should scan to pick up resources.
     *
     * @param resourcePackage the package to be scanned; not null
     * @return the builder
     * @throws NullPointerException if resourcePackage is null
     * @throws IllegalArgumentException if resourcePackage is empty
     * @see <a
     *     href="https://github.com/swagger-api/swagger-core/wiki/Swagger-Core-RESTEasy-2.X-Project-Setup#using-swaggers-beanconfig">Swagger's
     *     BeanConfig</a>
     */
    FinalBuilder addResourcePackage(String resourcePackage);

    /**
     * Adds the package of the given class to the packages Swagger should scan to pick up resources.
     *
     * @param resourcePackageClass the class whose package should be scanned; not null
     * @return the builder
     * @throws NullPointerException if resourcePackageClass is null
     * @see <a
     *     href="https://github.com/swagger-api/swagger-core/wiki/Swagger-Core-RESTEasy-2.X-Project-Setup#using-swaggers-beanconfig">Swagger's
     *     BeanConfig</a>
     */
    FinalBuilder addResourcePackageClass(Class<?> resourcePackageClass);
  }

  public interface FinalBuilder extends InitialBuilder {
    /**
     * Disables automatic addition of the embed query parameter if embeddable resources are
     * discovered.
     *
     * @return the builder.
     */
    FinalBuilder disableEmbedParameter();

    OpenApiBundle build();
  }

  public static final class Builder implements InitialBuilder, FinalBuilder {

    private final Set<String> resourcePackages;

    Builder() {
      resourcePackages = new LinkedHashSet<>();
      addResourcePackageClass(HalLinkDescriptionModifier.class);
      addResourcePackageClass(EmbedParameterModifier.class);
    }

    @Override
    public FinalBuilder disableEmbedParameter() {
      this.resourcePackages.remove(getResourcePackage(EmbedParameterModifier.class));
      return this;
    }

    @Override
    public Builder addResourcePackage(String resourcePackage) {
      notBlank(resourcePackage, "resourcePackage");
      resourcePackages.add(resourcePackage);
      return this;
    }

    @Override
    public Builder addResourcePackageClass(Class<?> resourcePackageClass) {
      resourcePackages.add(getResourcePackage(resourcePackageClass));
      return this;
    }

    @Override
    public OpenApiBundle build() {
      return new OpenApiBundle(resourcePackages);
    }

    private String getResourcePackage(Class<?> resourcePackageClass) {
      return requireNonNull(resourcePackageClass, "resourcePackageClass").getPackage().getName();
    }
  }
}
