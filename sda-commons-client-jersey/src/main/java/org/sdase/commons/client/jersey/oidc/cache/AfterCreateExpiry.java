package org.sdase.commons.client.jersey.oidc.cache;

import com.github.benmanes.caffeine.cache.Expiry;
import java.time.Duration;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sdase.commons.client.jersey.oidc.rest.model.TokenResource;

public class AfterCreateExpiry implements Expiry<String, TokenResource> {
  private static final int BUFFER_SECONDS = 5;

  /**
   * Entries in the cache should only be valid for the lifetime of the token as specified in the
   * expires_in property set by the issuer. To compensate for any delays the lifetime is reduced by
   * 5 seconds.
   */
  @Override
  public long expireAfterCreate(
      @NonNull String key, @NonNull TokenResource value, long currentTime) {
    long expirationTime = Duration.ofSeconds(value.getAccessTokenExpiresInSeconds()).toNanos();

    long buffer = Duration.ofSeconds(BUFFER_SECONDS).toNanos();
    return Math.max(0, expirationTime - buffer);
  }

  /**
   * Not necessary for the cache but needs to be implemented to satisfy the {@link Expiry}
   * interface. As stated in the {@link Expiry} documentation, the {@code currentDuration} may be
   * returned to not modify the expiration time.
   */
  @Override
  public long expireAfterUpdate(
      @NonNull String key,
      @NonNull TokenResource value,
      long currentTime,
      @NonNegative long currentDuration) {
    return currentDuration;
  }

  /**
   * Not necessary for the cache but needs to be implemented to satisfy the {@link Expiry}
   * interface. As stated in the {@link Expiry} documentation, the {@code currentDuration} may be
   * returned to not modify the expiration time.
   */
  @Override
  public long expireAfterRead(
      @NonNull String key,
      @NonNull TokenResource value,
      long currentTime,
      @NonNegative long currentDuration) {
    return currentDuration;
  }
}
