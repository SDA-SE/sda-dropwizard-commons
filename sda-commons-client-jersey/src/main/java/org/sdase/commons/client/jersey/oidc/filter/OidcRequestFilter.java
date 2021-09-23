/*
 * Copyright (c) 2018. SDA SE Open Industry Solutions (https://www.sda-se.com).
 *
 * All rights reserved.
 */
package org.sdase.commons.client.jersey.oidc.filter;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import org.sdase.commons.client.jersey.ClientFactory;
import org.sdase.commons.client.jersey.filter.AuthHeaderClientFilter;
import org.sdase.commons.client.jersey.oidc.OidcClient;
import org.sdase.commons.client.jersey.oidc.OidcConfiguration;
import org.sdase.commons.client.jersey.oidc.model.OidcResult;
import org.sdase.commons.client.jersey.oidc.model.OidcState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OidcRequestFilter implements ClientRequestFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(OidcRequestFilter.class);

  private OidcClient oidcClient;

  private AuthHeaderClientFilter authHeaderClientFilter;

  public OidcRequestFilter(
      ClientFactory clientFactory, OidcConfiguration oidc, boolean authenticationPassthrough) {
    if (authenticationPassthrough) {
      authHeaderClientFilter = new AuthHeaderClientFilter();
    }
    if (!oidc.isDisabled()) {
      this.oidcClient = new OidcClient(clientFactory, oidc);
    }
  }

  @Override
  public void filter(ClientRequestContext requestContext) {
    // Passing on existing tokens has precedence over creating new ones
    if (authHeaderClientFilter != null && authHeaderClientFilter.getHeaderValue().isPresent()) {
      return;
    }

    if (oidcClient != null && requestContext.getHeaderString(AUTHORIZATION) == null) {
      OidcResult oidcResult = oidcClient.createAccessToken();
      if (oidcResult.getState() == OidcState.OK) {
        requestContext.getHeaders().add(AUTHORIZATION, oidcResult.getBearerToken());
      } else {
        LOGGER.warn("Could not retrieve token. State was: {}", oidcResult.getState());
      }
    }
  }
}
