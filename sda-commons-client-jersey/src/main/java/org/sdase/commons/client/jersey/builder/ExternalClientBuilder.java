package org.sdase.commons.client.jersey.builder;

import io.dropwizard.client.JerseyClientBuilder;

public class ExternalClientBuilder extends AbstractBaseClientBuilder<ExternalClientBuilder> {

   public ExternalClientBuilder(JerseyClientBuilder jerseyClientBuilder) {
      super(jerseyClientBuilder);
   }

}
