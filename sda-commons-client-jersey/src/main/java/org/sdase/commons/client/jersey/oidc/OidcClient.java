package org.sdase.commons.client.jersey.oidc;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.sdase.commons.client.jersey.ClientFactory;
import org.sdase.commons.client.jersey.oidc.cache.AfterCreateExpiry;
import org.sdase.commons.client.jersey.oidc.model.OidcResult;
import org.sdase.commons.client.jersey.oidc.model.OidcState;
import org.sdase.commons.client.jersey.oidc.rest.IssuerClient;
import org.sdase.commons.client.jersey.oidc.rest.model.TokenResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Service that can retrieve a new access token. */
public class OidcClient {
  private static final Logger LOGGER = LoggerFactory.getLogger(OidcClient.class);
  private static final String CACHE_KEY = "oidcResultKey";

  private final OidcConfiguration config;
  private final Cache<String, TokenResource> cache;
  private final IssuerClient issuerClient;

  public OidcClient(ClientFactory clientFactory, OidcConfiguration config) {
    this.config = config;

    if (config.isDisabled()) {
      LOGGER.warn("OIDC was disabled.");
      this.cache = null;
      this.issuerClient = null;
      return;
    }

    this.issuerClient = new IssuerClient(clientFactory, config);

    if (config.getCache().isDisabled()) {
      this.cache = null;
      return;
    }

    this.cache = Caffeine.newBuilder().expireAfter(new AfterCreateExpiry()).build();
  }

  /**
   * Retrieves a new access token from the token endpoint using the given {@link OidcConfiguration}.
   */
  public synchronized OidcResult createAccessToken() {
    if (config.isDisabled()) {
      LOGGER.debug("OIDC client is disabled");
      return new OidcResult().setState(OidcState.SKIPPED);
    }

    TokenResource tokenResource;

    if (cache != null) {
      tokenResource = cache.get(CACHE_KEY, k -> issuerClient.getTokenResource());
    } else {
      tokenResource = issuerClient.getTokenResource();
    }

    if (tokenResource != null) {
      return new OidcResult().setState(OidcState.OK).setAccessToken(tokenResource.getAccessToken());
    } else {
      return new OidcResult().setState(OidcState.ERROR);
    }
  }

  /** Invalidate token in the cache. */
  public synchronized void clearCache() {
    if (cache != null) {
      cache.invalidateAll();
    }
  }
}
