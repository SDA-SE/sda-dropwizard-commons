/*
 * Copyright (c) 2017 Open API Tools
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Based on https://github.com/openapi-tools/swagger-hal/blob/05c00c9d5734731a1d08b4b43e0156279629a08d/src/test/java/io/openapitools/hal/HALModelConverterIT.java
 */
package org.sdase.commons.server.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.dropwizard.jackson.Jackson;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.jaxrs2.Reader;
import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Collections;
import java.util.Map;
import org.junit.Test;
import org.sdase.commons.server.openapi.apps.hal.AccountServiceExposure;
import org.sdase.commons.server.openapi.hal.HALModelResolver;

public class HALModelResolverIT {

  @Test
  public void testGeneration() throws OpenApiConfigurationException, JsonProcessingException {
    OpenAPI swagger = new OpenAPI();

    new JaxrsOpenApiContextBuilder()
        .openApiConfiguration(
            new SwaggerConfiguration()
                .resourcePackages(
                    Collections.singleton(AccountServiceExposure.class.getPackage().getName())))
        .buildContext(true);

    HALModelResolver halModelResolver = new HALModelResolver(Jackson.newObjectMapper());
    try {
      ModelConverters.getInstance().addConverter(halModelResolver);
      Reader reader = new Reader(swagger);
      swagger = reader.read(AccountServiceExposure.class);

      assertThat(swagger.getComponents().getSchemas()).containsKey("AccountRepresentation");

      String s =
          Jackson.newObjectMapper()
              .setSerializationInclusion(JsonInclude.Include.NON_NULL)
              .writeValueAsString(swagger);

      Schema accountRepresentation =
          swagger.getComponents().getSchemas().get("AccountRepresentation");
      Map<String, Schema> properties = accountRepresentation.getProperties();
      assertThat(accountRepresentation.getProperties())
          .containsKey("_links")
          .containsKey("_embedded")
          .hasSize(5);
      Schema transactions =
          ((Schema) ((Schema) properties.get("_embedded")).getProperties().get("transactions"));

      assertThat(transactions.getDescription())
          .isEqualTo("Embeds the latest transaction of account.");
    } finally {
      ModelConverters.getInstance().removeConverter(halModelResolver);
    }
  }
}
