/*
 * Copyright (c) 2018. SDA SE Open Industry Solutions (https://www.sda-se.com).
 *
 * All rights reserved.
 */
package org.sdase.commons.client.jersey.oidc.filter;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
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

  private final OidcConfiguration oidcConfiguration;

  private OidcClient oidcClient;

  private AuthHeaderClientFilter authHeaderClientFilter;

  public OidcRequestFilter(
      ClientFactory clientFactory, OidcConfiguration oidc, boolean authenticationPassthrough) {
    this.oidcConfiguration = oidc;
    if (authenticationPassthrough) {
      authHeaderClientFilter = new AuthHeaderClientFilter();
    }
    if (!oidc.isDisabled()) {
      this.oidcClient = new OidcClient(clientFactory, oidc);
      this.validateOidcConfig();
    }
  }

  public OidcRequestFilter(
      ClientFactory clientFactory,
      OidcConfiguration oidc,
      boolean authenticationPassthrough,
      String clientName) {
    this.oidcConfiguration = oidc;
    if (authenticationPassthrough) {
      authHeaderClientFilter = new AuthHeaderClientFilter();
    }
    if (!oidc.isDisabled()) {
      this.oidcClient = new OidcClient(clientFactory, oidc, clientName);
      this.validateOidcConfig();
    }
  }

  /**
   * Validates that an OIDC access token can be retrieved successfully.
   *
   * <p>This method is called during construction when OIDC is enabled. It enforces fast startup
   * failure for invalid OIDC settings, unreachable token endpoints, or credentials that cannot
   * retrieve a token.
   *
   * <p>Validation is skipped when {@link OidcConfiguration#isEnableStartupValidation()} returns
   * {@code false}. The constructors also skip this validation when OIDC is disabled.
   *
   * @throws IllegalStateException if startup validation is enabled and the OIDC client is not
   *     initialized, the token endpoint cannot be reached, or no valid access token can be
   *     retrieved
   */
  private void validateOidcConfig() {
    if (!oidcConfiguration.isEnableStartupValidation()) {
      return;
    }

    OidcResult oidcResult;
    try {
      oidcResult = oidcClient.createAccessToken();
    } catch (RuntimeException e) {
      throw new IllegalStateException(
          buildOidcConfigValidationErrorMessage("Token retrieval failed."), e);
    }

    if (oidcResult == null || oidcResult.getState() == OidcState.ERROR) {
      throw new IllegalStateException(
          buildOidcConfigValidationErrorMessage(
              "Could not retrieve access token. State was: "
                  + (oidcResult == null ? "null" : oidcResult.getState())));
    }
  }

  private String buildOidcConfigValidationErrorMessage(String detail) {
    return String.format(
        "OIDC startup validation failed. %s Configuration: issuerUrl='%s', grantType='%s'. Check OIDC provider settings.",
        detail, oidcConfiguration.getIssuerUrl(), oidcConfiguration.getGrantType());
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
