package org.sdase.commons.starter.builder;

import io.dropwizard.Configuration;

public interface OpenApiCustomizer {

  interface OpenApiInitialBuilder<C extends Configuration> extends PlatformBundleBuilder<C> {

    /**
     * Adds a package to the packages Swagger should scan to pick up resources.
     *
     * @param resourcePackage the package to be scanned; not null
     * @return the builder instance
     * @throws NullPointerException if resourcePackage is null
     * @throws IllegalArgumentException if resourcePackage is empty
     * @see org.sdase.commons.server.openapi.OpenApiBundle.InitialBuilder#addResourcePackage(String)
     */
    OpenApiFinalBuilder<C> addOpenApiResourcePackage(String resourcePackage);

    /**
     * Adds the package of the given class to the packages Swagger should scan to pick up resources.
     *
     * @param resourcePackageClass the class whose package should be scanned; not null
     * @return the builder instance
     * @throws NullPointerException if resourcePackageClass is null
     * @see
     *     org.sdase.commons.server.openapi.OpenApiBundle.InitialBuilder#addResourcePackageClass(Class)
     */
    OpenApiFinalBuilder<C> addOpenApiResourcePackageClass(Class<?> resourcePackageClass);

    /**
     * Use an existing OpenAPI 3 specification as base for the generation.
     *
     * <p>Note that the OpenAPI annotations always override values from the files if classes are
     * registered with {@link #addOpenApiResourcePackage(String)} or {@link
     * #addOpenApiResourcePackageClass(Class)}.
     *
     * @param openApiJsonOrYaml the OpenAPI 3 specification as json or yaml
     * @return the builder instance
     * @see
     *     org.sdase.commons.server.openapi.OpenApiBundle.InitialBuilder#withExistingOpenAPI(String)
     */
    OpenApiFinalBuilder<C> withExistingOpenAPI(String openApiJsonOrYaml);

    /**
     * Reads an existing OpenAPI 3 specification from the given classpath resource and provide it to
     * {@link #withExistingOpenAPI(String)}
     *
     * @param path the path to an OpenAPI 3 YAML or JSON file in the classpath.
     * @return the builder instance
     * @see
     *     org.sdase.commons.server.openapi.OpenApiBundle.InitialBuilder#withExistingOpenAPIFromClasspathResource(String)
     */
    OpenApiFinalBuilder<C> withExistingOpenAPIFromClasspathResource(String path);
  }

  interface OpenApiFinalBuilder<C extends Configuration>
      extends PlatformBundleBuilder<C>, OpenApiInitialBuilder<C> {}
}
