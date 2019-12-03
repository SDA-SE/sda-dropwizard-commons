package org.sdase.commons.server.swagger;

import com.google.common.annotations.VisibleForTesting;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.server.AbstractServerFactory;
import io.dropwizard.server.ServerFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiResponse;
import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import io.swagger.models.Contact;
import io.swagger.models.Info;
import io.swagger.models.License;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.sdase.commons.optional.server.swagger.parameter.embed.EmbedParameterModifier;
import org.sdase.commons.optional.server.swagger.json.example.JsonExampleModifier;
import org.sdase.commons.optional.server.swagger.sort.SortingModifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import java.lang.invoke.MethodHandles;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.MoreObjects.firstNonNull;
import static io.swagger.jaxrs.config.SwaggerContextService.*;
import static java.lang.String.join;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.Validate.notBlank;

/**
 * Configures the Swagger support.
 *
 * <h3>Example Usage</h3>
 * <h4>Minimal</h4>
 * <pre><code>
 *  public class ExampleApplication extends Application&lt;Configuration&gt; {
 *
 *    // ...
 *
 *    &#64;Override
 *    public void initialize(Bootstrap&lt;Configuration&gt; bootstrap) {
 *      // ...
 *      bootstrap.addBundle(
 *        SwaggerBundle.builder()
 *          .withTitle(getName())
 *          .addResourcePackageClass(getClass())
 *          .build());
 *    }
 *  }
 * </code></pre>
 *
 * <h4>All Customizations</h4>
 * <pre><code>
 *  public class ExampleApplication extends Application&lt;Configuration&gt; {
 *
 *    // ...
 *
 *    &#64;Override
 *    public void initialize(Bootstrap&lt;Configuration&gt; bootstrap) {
 *      // ...
 *      bootstrap.addBundle(
 *        SwaggerBundle.builder()
 *          .withTitle("Example Api)
 *          .addResourcePackageClass(Api.class)
 *          .addResourcePackage("org.sdase.commons.example.resources")
 *          .withVersion("1.2")
 *          .withDescription("Example Description")
 *          .withTermsOfServiceUrl("https://example.com/terms-of-service")
 *          .withContact("John Doe", "john.doe@example.com")
 *          .withLicense("Apache License", "https://www.apache.org/licenses/LICENSE-2.0.html")
 *          .build());
 *    }
 *  }
 * </code></pre>
 */
public final class SwaggerBundle implements ConfiguredBundle<Configuration> {

   private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
   private static final AtomicInteger UNIQUE_ID_COUNTER = new AtomicInteger();

   // https://www.dropwizard.io/0.9.1/docs/manual/configuration.html#man-configuration-all
   private static final String DROPWIZARD_DEFAULT_ROOT_PATH = "/*";

   private final Info info;
   private final String resourcePackages;

   private BeanConfig beanConfig;

   private SwaggerBundle(Info info, String resourcePackages) {
      this.info = info;
      this.resourcePackages = resourcePackages;
   }

   public static InitialBuilder builder() {
      return new Builder();
   }

   @Override
   public void initialize(Bootstrap<?> bootstrap) {
      // no initialization needed
   }

   @Override
   public void run(Configuration configuration, Environment environment) {
      // Get a new ID to register the swagger file in a unique context that is
      // not reused by another instance of this class.
      String instanceId = Integer.toString(UNIQUE_ID_COUNTER.incrementAndGet());

      String basePath = determineBasePath(configuration);

      beanConfig = new BeanConfig();
      beanConfig.setResourcePackage(resourcePackages);
      beanConfig.setBasePath(basePath);
      beanConfig.setPrettyPrint(true);

      // Set the config, context, and scanner ids to be used. These must be the
      // same as in {@link
      // ApiListingResourceWithDeducedHost.DelegatingServletConfig}.
      beanConfig.setConfigId(CONFIG_ID_PREFIX + instanceId);
      beanConfig.setContextId(CONTEXT_ID_KEY + "." + instanceId);
      beanConfig.setScannerId(SCANNER_ID_PREFIX + instanceId);
      //

      // order important
      beanConfig.setScan();
      beanConfig.getSwagger().getInfo().mergeWith(info);
      //

      environment.jersey().register(new ApiListingResourceWithDeducedHost(instanceId));
      environment.jersey().register(new SwaggerSerializers());

      // Allow CORS to access (via wildcard) from Swagger UI/editor
      String filterBasePath = basePath.endsWith("/") ? basePath : basePath + "/"; //NOSONAR
      FilterRegistration.Dynamic filter = environment.servlets().addFilter("CORS Swagger", CrossOriginFilter.class);
      filter.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class),
          true,
          filterBasePath + "swagger.yaml",
          filterBasePath + "swagger.json");
      filter.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
      filter.setInitParameter(CrossOriginFilter.ALLOW_CREDENTIALS_PARAM, Boolean.TRUE.toString());
      filter.setInitParameter(CrossOriginFilter.CHAIN_PREFLIGHT_PARAM, Boolean.FALSE.toString());

      LOG.info("Initialized Swagger with base path '{}' and resource packages: '{}'",
            beanConfig.getBasePath(), beanConfig.getResourcePackage());
   }

   @VisibleForTesting
   String getResourcePackages() {
      return resourcePackages;
   }

   @VisibleForTesting
   BeanConfig getBeanConfig() {
      return beanConfig;
   }

   private String determineBasePath(Configuration configuration) {
      ServerFactory serverFactory = configuration.getServerFactory();

      if (serverFactory instanceof AbstractServerFactory) {
         String basePath = ((AbstractServerFactory) serverFactory).getJerseyRootPath()
               .orElse(DROPWIZARD_DEFAULT_ROOT_PATH);

         // fix base path by removing '/*' at the end so swagger-ui can resolve it
         return "/*".equals(basePath)
               ? "/"
               : basePath.replaceAll("^(.*)/\\*$", "$1");
      }

      return "/api";
   }

   public interface InitialBuilder {

      /**
       * Sets the title of the API.
       *
       * @param title the title; not null or empty
       * @return the builder
       * @throws NullPointerException     if title is null
       * @throws IllegalArgumentException if title is empty
       * @see <a href="https://github.com/swagger-api/swagger-core/wiki/Swagger-Core-RESTEasy-2.X-Project-Setup#using-swaggers-beanconfig">Swagger's BeanConfig</a>
       */
      InterimBuilder withTitle(String title);
   }

   public interface InterimBuilder {

      /**
       * Adds a package to the packages Swagger should scan to pick up resources.
       *
       * @param resourcePackage the package to be scanned; not null
       * @return the builder
       * @throws NullPointerException     if resourcePackage is null
       * @throws IllegalArgumentException if resourcePackage is empty
       * @see <a href="https://github.com/swagger-api/swagger-core/wiki/Swagger-Core-RESTEasy-2.X-Project-Setup#using-swaggers-beanconfig">Swagger's BeanConfig</a>
       */
      FinalBuilder addResourcePackage(String resourcePackage);

      /**
       * Adds the package of the given class to the packages Swagger should scan to pick up resources.
       *
       * @param resourcePackageClass the class whose package should be scanned; not null
       * @return the builder
       * @throws NullPointerException if resourcePackageClass is null
       * @see <a href="https://github.com/swagger-api/swagger-core/wiki/Swagger-Core-RESTEasy-2.X-Project-Setup#using-swaggers-beanconfig">Swagger's BeanConfig</a>
       */
      FinalBuilder addResourcePackageClass(Class<?> resourcePackageClass);
   }

   public interface FinalBuilder extends InterimBuilder {

      /**
       * Disables automatic addition of the embed query parameter if embeddable resources are discovered.
       *
       * @return the builder.
       */
      FinalBuilder disableEmbedParameter();

      /**
       * Disables automatic rendering of Json examples in Swagger {@link ApiModelProperty#example() property examples}
       * and {@link ApiResponse#examples() response examples}. If disabled, only {@code String} and {@link Integer} are
       * recognized as special types.
       *
       * @return the builder
       */
      FinalBuilder disableJsonExamples();

      /**
       * Sets the version of the API.
       * <p>
       * Note: If no version is given (i.e. this method is not used) {@code 1.0} is used.
       *
       * @param version the version; not null or empty
       * @return the builder
       * @throws NullPointerException     if version is null
       * @throws IllegalArgumentException if version is empty
       * @see <a href="https://github.com/swagger-api/swagger-core/wiki/Swagger-Core-RESTEasy-2.X-Project-Setup#using-swaggers-beanconfig">Swagger's BeanConfig</a>
       */
      FinalBuilder withVersion(String version);

      /**
       * Sets the description of the API.
       *
       * @param description the description; not null or empty
       * @return the builder
       * @throws NullPointerException     if description is null
       * @throws IllegalArgumentException if description is empty
       * @see <a href="https://github.com/swagger-api/swagger-core/wiki/Swagger-Core-RESTEasy-2.X-Project-Setup#using-swaggers-beanconfig">Swagger's BeanConfig</a>
       */
      FinalBuilder withDescription(String description);

      /**
       * Sets the Terms of Service URL of the API.
       *
       * @param termsOfServiceUrl the Terms of Service URL; not null or empty
       * @return the builder
       * @throws NullPointerException     if termsOfServiceUrl is null
       * @throws IllegalArgumentException if termsOfServiceUrl is empty
       * @see <a href="https://github.com/swagger-api/swagger-core/wiki/Swagger-Core-RESTEasy-2.X-Project-Setup#using-swaggers-beanconfig">Swagger's BeanConfig</a>
       */
      FinalBuilder withTermsOfServiceUrl(String termsOfServiceUrl);

      /**
       * Sets the contact of the API.
       *
       * @param name the contact's name; not null or empty
       * @return the builder
       * @throws NullPointerException     if name is null
       * @throws IllegalArgumentException if name is empty
       * @see <a href="https://github.com/swagger-api/swagger-core/wiki/Swagger-Core-RESTEasy-2.X-Project-Setup#using-swaggers-beanconfig">Swagger's BeanConfig</a>
       */
      FinalBuilder withContact(String name);

      /**
       * Sets the contact of the API.
       *
       * @param name  the contact's name; not null or empty
       * @param email the contact's email; not null or empty
       * @return the builder
       * @throws NullPointerException     if name or email is null
       * @throws IllegalArgumentException if name or email is empty
       * @see <a href="https://github.com/swagger-api/swagger-core/wiki/Swagger-Core-RESTEasy-2.X-Project-Setup#using-swaggers-beanconfig">Swagger's BeanConfig</a>
       */
      FinalBuilder withContact(String name, String email);

      /**
       * Sets the contact of the API.
       *
       * @param name  the contact's name; not null or empty
       * @param email the contact's email; not null or empty
       * @param url   the contact's url; not null or empty
       * @return the builder
       * @throws NullPointerException     if name, email, or url is null
       * @throws IllegalArgumentException if name, email, or url is empty
       * @see <a href="https://github.com/swagger-api/swagger-core/wiki/Swagger-Core-RESTEasy-2.X-Project-Setup#using-swaggers-beanconfig">Swagger's BeanConfig</a>
       */
      FinalBuilder withContact(String name, String email, String url);

      /**
       * Sets the license of the API.
       *
       * @param name the license's name; not null or empty
       * @return the builder
       * @throws NullPointerException     if name is null
       * @throws IllegalArgumentException if name is empty
       * @see <a href="https://github.com/swagger-api/swagger-core/wiki/Swagger-Core-RESTEasy-2.X-Project-Setup#using-swaggers-beanconfig">Swagger's BeanConfig</a>
       */
      FinalBuilder withLicense(String name);

      /**
       * Sets the license of the API.
       *
       * @param name the license's name; not null or empty
       * @param url  the license's url; not null or empty
       * @return the builder
       * @throws NullPointerException     if name or url is null
       * @throws IllegalArgumentException if name or url is empty
       * @see <a href="https://github.com/swagger-api/swagger-core/wiki/Swagger-Core-RESTEasy-2.X-Project-Setup#using-swaggers-beanconfig">Swagger's BeanConfig</a>
       */
      FinalBuilder withLicense(String name, String url);

      SwaggerBundle build();
   }

   public static final class Builder implements InitialBuilder, FinalBuilder {

      private static final String DEFAULT_VERSION = "1";

      private final Set<String> resourcePackages;

      private String title;
      private String version;
      private String description;
      private String termsOfServiceUrl;
      private Contact contact;
      private License license;

      Builder() {
         resourcePackages = new LinkedHashSet<>();
         addResourcePackageClass(EmbedParameterModifier.class);
         addResourcePackageClass(JsonExampleModifier.class);
         addResourcePackageClass(SortingModifier.class);
      }

      @Override
      public FinalBuilder disableEmbedParameter() {
         this.resourcePackages.remove(getResourcePackage(EmbedParameterModifier.class));
         return this;
      }

      @Override
      public FinalBuilder disableJsonExamples() {
         this.resourcePackages.remove(getResourcePackage(JsonExampleModifier.class));
         return this;
      }

      @Override
      public InterimBuilder withTitle(String title) {
         notBlank(title, "title");
         this.title = title;
         return this;
      }

      @Override
      public FinalBuilder withVersion(String version) {
         notBlank(version, "version");
         this.version = version;
         return this;
      }

      @Override
      public FinalBuilder withDescription(String description) {
         notBlank(description, "description");
         this.description = description;
         return this;
      }

      @Override
      public FinalBuilder withTermsOfServiceUrl(String termsOfServiceUrl) {
         notBlank(termsOfServiceUrl, "termsOfServiceUrl");
         this.termsOfServiceUrl = termsOfServiceUrl;
         return this;
      }

      @Override
      public FinalBuilder withContact(String name) {
         notBlank(name, "name");
         contact = new Contact().name(name);
         return this;
      }

      @Override
      public FinalBuilder withContact(String name, String email) {
         notBlank(name, "name");
         notBlank(email, "email");
         contact = new Contact().name(name).email(email);
         return this;
      }

      @Override
      public FinalBuilder withContact(String name, String email, String url) {
         notBlank(name, "name");
         notBlank(email, "email");
         notBlank(url, "url");
         contact = new Contact().name(name).email(email).url(url);
         return this;
      }

      @Override
      public FinalBuilder withLicense(String name) {
         notBlank(name, "name");
         license = new License().name(name);
         return this;
      }

      @Override
      public FinalBuilder withLicense(String name, String url) {
         notBlank(name, "name");
         notBlank(url, "url");
         license = new License().name(name).url(url);
         return this;
      }

      @Override
      public FinalBuilder addResourcePackage(String resourcePackage) {
         notBlank(resourcePackage, "resourcePackage");
         resourcePackages.add(resourcePackage);
         return this;
      }

      @Override
      public FinalBuilder addResourcePackageClass(Class<?> resourcePackageClass) {
         resourcePackages.add(getResourcePackage(resourcePackageClass));
         return this;
      }

      @Override
      public SwaggerBundle build() {
         Info info = new Info();

         info.setTitle(title);
         info.setVersion(firstNonNull(version, DEFAULT_VERSION));
         info.setDescription(description);
         info.setTermsOfService(termsOfServiceUrl);
         info.setContact(contact);
         info.setLicense(license);

         return new SwaggerBundle(info, join(",", resourcePackages));
      }

      private String getResourcePackage(Class<?> resourcePackageClass) {
         return requireNonNull(resourcePackageClass, "resourcePackageClass").getPackage().getName();
      }
   }
}
