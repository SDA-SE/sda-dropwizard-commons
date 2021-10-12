package org.sdase.commons.server.auth.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.client.JerseyClientConfiguration;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;

/** Configuration for authentication using JWT. */
public class AuthConfig {

  /** The client configuration of the HTTP client that is used for the key loader. */
  private JerseyClientConfiguration keyLoaderClient;

  /** Keys that are allowed to sign tokens derived from {@link #setKeys(List)}. */
  private final List<KeyLocation> keys = new ArrayList<>();

  /** Keys that are allowed to sign tokens derived from {@link #setIssuers(String)}. */
  @JsonIgnore private final List<KeyLocation> keysFromIssuers = new ArrayList<>();

  /**
   * The default window in seconds in which the Not Before, Issued At and Expires At Claims will
   * still be valid.
   */
  private long leeway = 0;

  /**
   * Used to disable authentication for local development and unit testing. Should NEVER be set in
   * production.
   */
  private boolean disableAuth;

  public JerseyClientConfiguration getKeyLoaderClient() {
    return keyLoaderClient;
  }

  public AuthConfig setKeyLoaderClient(JerseyClientConfiguration keyLoaderClient) {
    this.keyLoaderClient = keyLoaderClient;
    return this;
  }

  public List<KeyLocation> getKeys() {
    List<KeyLocation> combinedKeys = new ArrayList<>();
    combinedKeys.addAll(keys);
    combinedKeys.addAll(keysFromIssuers);
    return combinedKeys;
  }

  public AuthConfig setKeys(List<KeyLocation> keys) {
    this.keys.clear();
    if (keys != null) {
      this.keys.addAll(keys);
    }
    return this;
  }

  public long getLeeway() {
    return leeway;
  }

  public AuthConfig setLeeway(long leeway) {
    this.leeway = leeway;
    return this;
  }

  public boolean isDisableAuth() {
    return disableAuth;
  }

  public AuthConfig setDisableAuth(boolean disableAuth) {
    this.disableAuth = disableAuth;
    return this;
  }

  public AuthConfig setIssuers(String commaSeparatedIssuers) {
    this.keysFromIssuers.clear();
    if (commaSeparatedIssuers != null) {
      Stream.of(commaSeparatedIssuers.split(","))
          .filter(StringUtils::isNotBlank)
          .map(String::trim)
          .map(
              iss ->
                  new KeyLocation()
                      .setType(KeyUriType.OPEN_ID_DISCOVERY)
                      .setLocation(URI.create(iss))
                      .setRequiredIssuer(iss))
          .forEach(this.keysFromIssuers::add);
    }
    return this;
  }
}
